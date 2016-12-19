package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.properties.global.GlobalSettingsUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.StringUtils;

public final class DefaultGlobalSettingsFileManager implements GlobalSettingsFileManager {
    private static final Logger LOGGER = Logger.getLogger(DefaultGlobalSettingsFileManager.class.getName());

    private static final TaskExecutor SETTINGS_FILE_UPDATER = NbTaskExecutors.newDefaultFifoExecutor();

    private final RootProjectRegistry rootProjectRegistry;
    private final UpdateTaskExecutor settingsDefPersistor;

    private final Lock outstandingDefsLock;
    private final Map<File, SettingsDef> outstandingDefs;

    public DefaultGlobalSettingsFileManager(RootProjectRegistry rootProjectRegistry) {
        ExceptionHelper.checkNotNullArgument(rootProjectRegistry, "rootProjectRegistry");
        this.rootProjectRegistry = rootProjectRegistry;
        this.settingsDefPersistor = new GenericUpdateTaskExecutor(SETTINGS_FILE_UPDATER);
        this.outstandingDefsLock = new ReentrantLock();
        this.outstandingDefs = new HashMap<>();
    }

    @Override
    public SettingsGradleDef tryGetSettingsFile(File projectDir) {
        if (NbGradleModel.isBuildSrcDirectory(projectDir)) {
            return SettingsGradleDef.NO_SETTINGS;
        }

        Path explicitSettingsFile = rootProjectRegistry.tryGetSettingsFile(projectDir);
        if (explicitSettingsFile != null) {
            return new SettingsGradleDef(explicitSettingsFile, false);
        }

        SettingsDef result = tryGetSettingsDef(projectDir);
        if (result == null) {
            return null;
        }

        return result.settingsGradleDef;
    }

    private void setAllSettingsDef(NbGradleProjectTree root, SettingsGradleDef settingsDef, long timeStamp) {
        SettingsDef def = new SettingsDef(root.getProjectDir(), settingsDef, timeStamp);

        outstandingDefsLock.lock();
        try {
            outstandingDefs.put(root.getProjectDir(), def);
        } finally {
            outstandingDefsLock.unlock();
        }

        for (NbGradleProjectTree child: root.getChildren()) {
            setAllSettingsDef(child, settingsDef, timeStamp);
        }
    }

    private void setAllSettingsDef(NbGradleModel model, long timeStamp) {
        NbGradleProjectTree root = model.getProjectDef().getRootProject();
        SettingsGradleDef settingsDef = model.getSettingsGradleDef();
        setAllSettingsDef(root, settingsDef, timeStamp);
    }

    @Override
    public void updateSettingsFile(NbGradleModel model) {
        ExceptionHelper.checkNotNullArgument(model, "model");
        setAllSettingsDef(model, System.currentTimeMillis());

        settingsDefPersistor.execute(new Runnable() {
            @Override
            public void run() {
                persistSettingsDefsNow();
            }
        });
    }

    private void persistSettingsDefsNow() {
        try {
            persistSettingsDefsNow0();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void persistSettingsDefsNow0() throws IOException {
        List<SettingsDef> toSave;
        outstandingDefsLock.lock();
        try {
            if (outstandingDefs.isEmpty()) {
                return;
            }

            toSave = new ArrayList<>(outstandingDefs.values());
        } finally {
            outstandingDefsLock.unlock();
        }

        MessageDigest hashCalculator = getNameHasher();
        for (SettingsDef def: toSave) {
            Path savePath = tryGetProjectSaveFile(def.projectDir, hashCalculator);
            if (savePath == null) {
                LOGGER.log(Level.WARNING, "Cannot save settings.gradle location for projects.");
                break;
            }

            Path settingsGradle = def.settingsGradleDef.getSettingsGradle();

            Properties output = new Properties();
            output.put("maySearchUpwards", Boolean.toString(def.settingsGradleDef.isMaySearchUpwards()));
            output.put("settingsGradle", settingsGradle != null ? settingsGradle.toString() : "");
            output.put("timeStamp", Long.toString(def.timeStamp));

            Files.createDirectories(savePath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(savePath)) {
                output.store(outputStream, null);
            }
        }

        outstandingDefsLock.lock();
        try {
            for (SettingsDef def: toSave) {
                if (outstandingDefs.get(def.projectDir) == def) {
                    outstandingDefs.remove(def.projectDir);
                }
            }
        } finally {
            outstandingDefsLock.unlock();
        }
    }

    private SettingsDef tryGetSettingsDef(File projectDir) {
        outstandingDefsLock.lock();
        try {
            SettingsDef outstanding = outstandingDefs.get(projectDir);
            if (outstanding != null) {
                return outstanding;
            }
        } finally {
            outstandingDefsLock.unlock();
        }

        SettingsDef result = tryGetStoredSettingsDef(projectDir);

        outstandingDefsLock.lock();
        try {
            SettingsDef outstanding = outstandingDefs.get(projectDir);
            if (outstanding != null) {
                result = outstanding;
            }
        } finally {
            outstandingDefsLock.unlock();
        }

        return result;
    }

    private SettingsDef tryGetStoredSettingsDef(File projectDir) {
        Path settingsFile = tryGetProjectSaveFile(projectDir);
        if (settingsFile == null || !Files.isRegularFile(settingsFile)) {
            return null;
        }

        Properties settings = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            settings.load(input);
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Failed to load settings from: " + settingsFile, ex);
            return null;
        }

        String maySearchUpwards = settings.getProperty("maySearchUpwards", "");
        String settingsGradle = settings.getProperty("settingsGradle", "");
        String timeStamp = settings.getProperty("timeStamp", "");

        try {
            SettingsGradleDef settingsGradleDef = new SettingsGradleDef(
                    settingsGradle.isEmpty() ? Paths.get(settingsGradle) : null,
                    Boolean.parseBoolean(maySearchUpwards));

            return new SettingsDef(
                    projectDir,
                    settingsGradleDef,
                    Long.parseLong(timeStamp));
        } catch (InvalidPathException | NumberFormatException ex) {
            LOGGER.log(Level.INFO, "Failed to parse settings settings in: " + settingsFile, ex);
            return null;
        }
    }

    private Path tryGetProjectSaveFile(File projectDir) {
        return tryGetProjectSaveFile(projectDir, getNameHasher());
    }

    private Path tryGetProjectSaveFile(File projectDir, MessageDigest hashCalculator) {
        hashCalculator.reset();
        String keyHash = StringUtils.byteArrayToHex(hashCalculator.digest(projectDir.toString().getBytes(StringUtils.UTF8)));

        List<String> subPaths = new ArrayList<>();
        subPaths.add("settings-gradle");

        if (keyHash.length() > 2) {
            subPaths.add(keyHash.substring(0, 2));
            subPaths.add(keyHash.substring(2) + ".properties");
        }
        else {
            subPaths.add("XX");
            subPaths.add(keyHash + ".properties");
        }

        return GlobalSettingsUtils.tryGetGlobalConfigPath(subPaths);
    }

    private static MessageDigest getNameHasher() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to load the MD5 calculator.", ex);
        }
    }

    private static final class SettingsDef {
        public final File projectDir;
        public final SettingsGradleDef settingsGradleDef;
        public final long timeStamp;

        public SettingsDef(File projectDir, SettingsGradleDef settingsGradleDef, long timeStamp) {
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
            ExceptionHelper.checkNotNullArgument(settingsGradleDef, "settingsGradleDef");

            this.projectDir = projectDir;
            this.settingsGradleDef = settingsGradleDef;
            this.timeStamp = timeStamp;
        }
    }
}
