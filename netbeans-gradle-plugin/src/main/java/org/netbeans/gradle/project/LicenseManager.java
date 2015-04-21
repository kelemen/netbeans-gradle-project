package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.MonitorableTaskExecutor;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.util.CloseableAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class LicenseManager {
    private static final Logger LOGGER = Logger.getLogger(LicenseManager.class.getName());

    private static final MonitorableTaskExecutor SYNC_EXECUTOR
            = NbTaskExecutors.newDefaultFifoExecutor();

    // The key File does not contain a path we only use File to properly
    // use case insensitivity if required.
    private final Map<File, AtomicInteger> useCount;

    public LicenseManager() {
        this.useCount = new HashMap<>();
    }

    private FileObject getLicenseRoot() {
        FileObject configRoot = FileUtil.getConfigRoot();
        return configRoot != null
                ? configRoot.getFileObject("Templates/Licenses")
                : null;
    }

    private void removeLicense(File file) throws IOException {
        assert SYNC_EXECUTOR.isExecutingInThis();

        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        FileObject licenseFile = licenseRoot.getFileObject(file.getPath());
        if (licenseFile == null) {
            LOGGER.log(Level.INFO, "License file does not exist: {0}", file);
            return;
        }
        licenseFile.delete();
    }

    private void addLicense(File file, NbGradleProject project, LicenseHeaderInfo header) throws IOException {
        assert SYNC_EXECUTOR.isExecutingInThis();

        FileObject licenseRoot = getLicenseRoot();
        if (licenseRoot == null) {
            LOGGER.warning("License root does not exist.");
            return;
        }

        if (licenseRoot.getFileObject(file.getPath()) != null) {
            LOGGER.log(Level.INFO, "License file already exists: {0}", file);
            return;
        }

        project.waitForLoadedProject(Cancellation.UNCANCELABLE_TOKEN);
        Path licenseTemplateFile = header.getLicenseTemplateFile(project);
        if (licenseTemplateFile == null) {
            return;
        }

        licenseTemplateFile = licenseTemplateFile.normalize();
        if (licenseTemplateFile.getNameCount() == 0) {
            return;
        }

        FileObject licenseTemplateSrc = FileUtil.toFileObject(licenseTemplateFile.toFile());
        if (licenseTemplateSrc == null) {
            LOGGER.log(Level.WARNING, "Missing license template file: {0}", licenseTemplateFile);
            return;
        }

        licenseTemplateSrc.copy(licenseRoot, file.getPath(), "");
    }

    private void doUnregister(final File file) {
        SYNC_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                AtomicInteger fileCount = useCount.get(file);
                if (fileCount == null) {
                    LOGGER.log(Level.WARNING, "Too many unregister call to LicenseManager.", new Exception());
                    return;
                }

                if (fileCount.decrementAndGet() == 0) {
                    useCount.remove(file);
                    removeLicense(file);
                }
            }
        }, null);
    }

    private void doRegister(final File file, final NbGradleProject project, final LicenseHeaderInfo header) {
        SYNC_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws IOException {
                AtomicInteger fileCount = useCount.get(file);
                if (fileCount == null) {
                    fileCount = new AtomicInteger(1);
                    useCount.put(file, fileCount);
                }
                else {
                    fileCount.incrementAndGet();
                }

                if (fileCount.get() == 1) {
                    addLicense(file, project, header);
                }
            }
        }, null);
    }

    private CloseableAction getRegisterListenerAction(
            final NbGradleProject project,
            final LicenseHeaderInfo header) {
        assert project != null;

        return new CloseableAction() {
            @Override
            public CloseableAction.Ref open() {
                return registerLicense(project, header);
            }
        };
    }

    public PropertySource<CloseableAction> getRegisterListenerAction(
            final NbGradleProject project,
            PropertySource<? extends LicenseHeaderInfo> headerProperty) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(headerProperty, "headerProperty");

        return PropertyFactory.convert(headerProperty, new ValueConverter<LicenseHeaderInfo, CloseableAction>() {
            @Override
            public CloseableAction convert(LicenseHeaderInfo input) {
                return getRegisterListenerAction(project, input);
            }
        });
    }

    private CloseableAction.Ref registerLicense(NbGradleProject project, LicenseHeaderInfo header) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        if (header == null) {
            return CloseableAction.CLOSED_REF;
        }

        if (header.getLicenseTemplateFile() == null) {
            return CloseableAction.CLOSED_REF;
        }

        final File licenseFile = getLicenseFileName(project, header);
        doRegister(licenseFile, project, header);

        return new CloseableAction.Ref() {
            private final AtomicBoolean unregistered = new AtomicBoolean(false);

            @Override
            public void close() {
                if (unregistered.compareAndSet(false, true)) {
                    doUnregister(licenseFile);
                }
            }
        };
    }

    private static File getLicenseFileName(NbGradleProject project, LicenseHeaderInfo header) {
        return new File("license-" + header.getPrivateLicenseName(project) + ".txt");
    }
}
