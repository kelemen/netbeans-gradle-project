package org.netbeans.gradle.project;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.MonitorableTaskExecutor;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.CloseableAction;
import org.netbeans.gradle.project.util.NbBiFunction;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class LicenseManager {
    private static final Logger LOGGER = Logger.getLogger(LicenseManager.class.getName());
    private static final Random RND = new SecureRandom();

    private static final MonitorableTaskExecutor SYNC_EXECUTOR
            = NbTaskExecutors.newDefaultFifoExecutor();

    private final Map<LicenseKey, RegisteredLicense> licenseRegistartions;

    public LicenseManager() {
        this.licenseRegistartions = new ConcurrentHashMap<>();
    }

    public String tryGetRegisteredLicenseName(NbGradleModel projectModel, LicenseHeaderInfo headerInfo) {
        ExceptionHelper.checkNotNullArgument(projectModel, "projectModel");
        ExceptionHelper.checkNotNullArgument(headerInfo, "headerInfo");

        LicenseKey key = new LicenseKey(projectModel, headerInfo);
        RegisteredLicense registration = licenseRegistartions.get(key);
        String licenseName = registration != null
                ? registration.privateName
                : headerInfo.getLicenseName();

        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return null;
        }

        String fileName = toLicenseFileName(licenseName);
        if (FileUtil.getConfigFile("Templates/Licenses/" + fileName) == null) {
            return null;
        }

        return licenseName;
    }

    private FileObject getLicenseRoot() {
        FileObject configRoot = FileUtil.getConfigRoot();
        return configRoot != null
                ? configRoot.getFileObject("Templates/Licenses")
                : null;
    }

    private void removeLicense(RegisteredLicense registration) throws IOException {
        assert SYNC_EXECUTOR.isExecutingInThis();

        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        FileObject licenseFile = licenseRoot.getFileObject(registration.baseFileName);
        if (licenseFile == null) {
            LOGGER.log(Level.INFO, "License file does not exist: {0}", registration.baseFileName);
            return;
        }
        licenseFile.delete();
    }

    private void addLicense(NbGradleModel projectModel, RegisteredLicense registration) throws IOException {
        assert SYNC_EXECUTOR.isExecutingInThis();

        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        if (licenseRoot.getFileObject(registration.baseFileName) != null) {
            LOGGER.log(Level.INFO, "License file already exists: {0}", registration.baseFileName);
            return;
        }

        Path licenseTemplateFile = registration.key.getAbsoluteSrcFile(projectModel);
        licenseTemplateFile = licenseTemplateFile.normalize();

        if (licenseTemplateFile.getNameCount() == 0) {
            return;
        }

        FileObject licenseTemplateSrc = FileUtil.toFileObject(licenseTemplateFile.toFile());
        if (licenseTemplateSrc == null) {
            LOGGER.log(Level.WARNING, "Missing license template file: {0}", licenseTemplateFile);
            return;
        }

        FileObject templateFile = licenseTemplateSrc.copy(licenseRoot, registration.baseFileName, "");
        templateFile.setAttribute("template", true);

        String projectName = projectModel.getMainProject().getGenericProperties().getProjectFullName();
        templateFile.setAttribute("displayName", registration.key.name + " (" + projectName + ")");
    }

    private void doUnregister(final LicenseKey key) {
        SYNC_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                RegisteredLicense registration = licenseRegistartions.get(key);
                if (registration == null) {
                    LOGGER.log(Level.WARNING, "Too many unregister call to LicenseManager.", new Exception());
                    return;
                }

                if (registration.release()) {
                    licenseRegistartions.remove(key);
                    removeLicense(registration);
                }
            }
        }, null);
    }

    private void doRegister(final NbGradleModel projectModel, final LicenseKey key) {
        SYNC_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                RegisteredLicense registration = licenseRegistartions.get(key);
                if (registration == null) {
                    registration = new RegisteredLicense(key);
                    licenseRegistartions.put(key, registration);
                    addLicense(projectModel, registration);
                }
                else {
                    registration.use();
                }
            }
        }, null);
    }

    private CloseableAction getRegisterListenerAction(
            final NbGradleModel projectModel,
            final LicenseHeaderInfo header) {
        ExceptionHelper.checkNotNullArgument(projectModel, "projectModel");

        return new CloseableAction() {
            @Override
            public CloseableAction.Ref open() {
                return registerLicense(projectModel, header);
            }
        };
    }

    public PropertySource<CloseableAction> getRegisterListenerAction(
            PropertySource<? extends NbGradleModel> projectModelProperty,
            PropertySource<? extends LicenseHeaderInfo> headerProperty) {
        ExceptionHelper.checkNotNullArgument(projectModelProperty, "projectModelProperty");
        ExceptionHelper.checkNotNullArgument(headerProperty, "headerProperty");

        return NbProperties.combine(headerProperty, projectModelProperty, new NbBiFunction<LicenseHeaderInfo, NbGradleModel, CloseableAction>() {
            @Override
            public CloseableAction apply(LicenseHeaderInfo headerInfo, NbGradleModel projectModel) {
                return getRegisterListenerAction(projectModel, headerInfo);
            }
        });
    }

    private CloseableAction.Ref registerLicense(NbGradleModel projectModel, LicenseHeaderInfo header) {
        ExceptionHelper.checkNotNullArgument(projectModel, "projectModel");

        if (header == null) {
            return CloseableAction.CLOSED_REF;
        }

        final Path templateFile = header.getLicenseTemplateFile();
        if (templateFile == null) {
            return CloseableAction.CLOSED_REF;
        }

        final LicenseKey key = new LicenseKey(projectModel, header);

        doRegister(projectModel, key);

        return new CloseableAction.Ref() {
            private final AtomicBoolean unregistered = new AtomicBoolean(false);

            @Override
            public void close() {
                if (unregistered.compareAndSet(false, true)) {
                    doUnregister(key);
                }
            }
        };
    }

    private static String toLicenseFileName(String licenseName) {
        // This naming patter in required by NetBeans
        return "license-" + licenseName + ".txt";
    }

    private static final class LicenseKey {
        private final Path rootDir;
        private final Path srcFile;
        private final String name;

        public LicenseKey(NbGradleModel projectModel, LicenseHeaderInfo headerInfo) {
            this.rootDir = projectModel.getSettingsDir();
            this.srcFile = headerInfo.getLicenseTemplateFile();
            this.name = headerInfo.getLicenseName();
        }

        public Path getAbsoluteSrcFile(NbGradleModel currentModel) {
            return rootDir.resolve(srcFile);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.rootDir);
            hash = 97 * hash + Objects.hashCode(this.srcFile);
            hash = 97 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final LicenseKey other = (LicenseKey)obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.srcFile, other.srcFile)
                    && Objects.equals(this.rootDir, other.rootDir);
        }
    }

    private static final class RegisteredLicense {
        private final LicenseKey key;
        private final String privateName;
        private final String baseFileName;
        private int useCount;

        public RegisteredLicense(LicenseKey key) {
            this.key = key;
            this.useCount = 1;

            String safeName = NbFileUtils.toSafeFileName(key.name);
            String randomStr = Long.toHexString(RND.nextLong()) + "-" + Long.toHexString(RND.nextLong());
            this.privateName = "nb-gradle-" + safeName + "-" + randomStr;
            // This naming patter in required by NetBeans
            this.baseFileName = toLicenseFileName(privateName);
        }

        public void use() {
            useCount++;
        }

        public boolean release() {
            useCount--;
            return useCount <= 0;
        }
    }
}
