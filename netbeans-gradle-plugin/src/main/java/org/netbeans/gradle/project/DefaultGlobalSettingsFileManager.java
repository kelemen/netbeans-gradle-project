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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.properties.global.GlobalSettingsUtils;
import org.netbeans.gradle.project.util.LazyPaths;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.StringUtils;

public final class DefaultGlobalSettingsFileManager implements GlobalSettingsFileManager {
    private static final Logger LOGGER = Logger.getLogger(DefaultGlobalSettingsFileManager.class.getName());

    private static final TaskExecutor SETTINGS_FILE_UPDATER = NbTaskExecutors.newDefaultFifoExecutor();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int STAMP_SIZE = 16 ; // bytes

    private final LazyPaths cacheDir;
    private final RootProjectRegistry rootProjectRegistry;
    private final UpdateTaskExecutor settingsDefPersistor;

    private final Lock outstandingDefsLock;
    private final Map<File, SettingsDef> outstandingDefs;

    private final Locker locker;

    public DefaultGlobalSettingsFileManager(RootProjectRegistry rootProjectRegistry) {
        this(rootProjectRegistry, GlobalSettingsUtils.cacheRoot());
    }

    public DefaultGlobalSettingsFileManager(RootProjectRegistry rootProjectRegistry, LazyPaths cacheDir) {
        this.rootProjectRegistry = Objects.requireNonNull(rootProjectRegistry, "rootProjectRegistry");
        this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir");
        this.settingsDefPersistor = new GenericUpdateTaskExecutor(SETTINGS_FILE_UPDATER);
        this.outstandingDefsLock = new ReentrantLock();
        this.outstandingDefs = new HashMap<>();
        this.locker = new Locker();
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

    private static void putAllSettingsDef(
            File rootProjectDir,
            NbGradleProjectTree root,
            SettingsGradleDef settingsDef,
            String stamp,
            Map<File, SettingsDef> result) {

        File projectDir = root.getProjectDir();
        SettingsDef def = new SettingsDef(rootProjectDir, projectDir, settingsDef, stamp);
        result.put(projectDir, def);

        for (NbGradleProjectTree child: root.getChildren()) {
            putAllSettingsDef(rootProjectDir, child, settingsDef, stamp, result);
        }
    }

    private void setAllSettingsDef(NbGradleModel model, String stamp) {
        NbGradleProjectTree root = model.getProjectDef().getRootProject();
        SettingsGradleDef settingsDef = model.getSettingsGradleDef();
        File rootProjectDir = root.getProjectDir();

        outstandingDefsLock.lock();
        try {
            putAllSettingsDef(rootProjectDir, root, settingsDef, stamp, outstandingDefs);
        } finally {
            outstandingDefsLock.unlock();
        }
    }

    private String getStamp() {
        byte[] rawStamp = new byte[STAMP_SIZE];
        RANDOM.nextBytes(rawStamp);

        return StringUtils.byteArrayToHex(rawStamp);
    }

    @Override
    public void updateSettingsFile(NbGradleModel model) {
        ExceptionHelper.checkNotNullArgument(model, "model");
        setAllSettingsDef(model, getStamp());

        settingsDefPersistor.execute(new Runnable() {
            @Override
            public void run() {
                persistSettingsDefsNow();
            }
        });
    }

    // For test only
    void waitForOutstanding(long msToWait) {
        final WaitableSignal signal = new WaitableSignal();
        SETTINGS_FILE_UPDATER.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                signal.signal();
            }
        }, null);

        if (!signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, msToWait, TimeUnit.MILLISECONDS)) {
            throw new OperationCanceledException("timeout");
        }
    }

    private void persistSettingsDefsNow() {
        try {
            persistSettingsDefsNow0();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void persistSettingsDefsNow0() throws IOException {
        final List<SettingsDef> toSave;
        outstandingDefsLock.lock();
        try {
            if (outstandingDefs.isEmpty()) {
                return;
            }

            toSave = new ArrayList<>(outstandingDefs.values());
        } finally {
            outstandingDefsLock.unlock();
        }

        getLocker().doWrite(new IoTask<Void>() {
            @Override
            public Void run() throws IOException {
                MessageDigest hashCalculator = getNameHasher();
                for (SettingsDef def: toSave) {
                    Path savePath = tryGetProjectSaveFile(def.projectDir, hashCalculator);
                    if (savePath == null) {
                        LOGGER.log(Level.WARNING, "Cannot save settings.gradle location for projects.");
                        break;
                    }

                    Path settingsGradle = def.settingsGradleDef.getSettingsGradle();

                    Properties output = new Properties();
                    output.put("projectDir", def.projectDir.toString());
                    output.put("rootProjectDir", def.rootProjectDir.toString());
                    output.put("maySearchUpwards", Boolean.toString(def.settingsGradleDef.isMaySearchUpwards()));
                    output.put("settingsGradle", settingsGradle != null ? settingsGradle.toString() : "");
                    output.put("stamp", def.stamp);

                    Files.createDirectories(savePath.getParent());
                    try (OutputStream outputStream = Files.newOutputStream(savePath)) {
                        output.store(outputStream, null);
                    }
                }
                return null;
            }
        });

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
        try {
            return tryGetStoredSettingsDef0(projectDir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private SettingsDef tryGetStoredSettingsDef0(final File projectDir) throws IOException {
        return getLocker().doRead(new IoTask<SettingsDef>() {
            @Override
            public SettingsDef run() throws IOException {
                SettingsDef result = tryGetStoredSettingsDefUnsafe(projectDir);
                if (result == null) {
                    return null;
                }

                if (Objects.equals(projectDir, result.projectDir)) {
                    return result;
                }

                SettingsDef rootDef = tryGetStoredSettingsDefUnsafe(projectDir);
                if (rootDef == null) {
                    return null;
                }

                return Objects.equals(result.stamp, rootDef.stamp) ? result : null;
            }
        });
    }

    private SettingsDef tryGetStoredSettingsDefUnsafe(File projectDir) {
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

        String storedProjectDir = settings.getProperty("projectDir", "");
        if (!storedProjectDir.isEmpty() && !projectDir.equals(new File(storedProjectDir))) {
            return null;
        }

        String rootProjectDir = settings.getProperty("rootProjectDir", "");
        String maySearchUpwards = settings.getProperty("maySearchUpwards", "");
        String settingsGradle = settings.getProperty("settingsGradle", "");
        String stamp = settings.getProperty("stamp", "");

        try {
            SettingsGradleDef settingsGradleDef = new SettingsGradleDef(
                    settingsGradle.isEmpty() ? null : Paths.get(settingsGradle),
                    Boolean.parseBoolean(maySearchUpwards));

            return new SettingsDef(new File(rootProjectDir), projectDir, settingsGradleDef, stamp);
        } catch (InvalidPathException ex) {
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

        return cacheDir.tryGetSubPath(subPaths);
    }

    private Locker getLocker() {
        return locker;
    }

    private static MessageDigest getNameHasher() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Unable to load the MD5 calculator.", ex);
        }
    }

    private static final class SettingsDef {
        public final File rootProjectDir;
        public final File projectDir;
        public final SettingsGradleDef settingsGradleDef;
        public final String stamp;

        public SettingsDef(
                File rootProjectDir,
                File projectDir,
                SettingsGradleDef settingsGradleDef,
                String stamp) {
            ExceptionHelper.checkNotNullArgument(rootProjectDir, "rootProjectDir");
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
            ExceptionHelper.checkNotNullArgument(settingsGradleDef, "settingsGradleDef");
            ExceptionHelper.checkNotNullArgument(stamp, "stamp");

            this.rootProjectDir = rootProjectDir;
            this.projectDir = projectDir;
            this.settingsGradleDef = settingsGradleDef;
            this.stamp = stamp;
        }
    }

    private static final class Locker {
        private final Lock readLock;
        private final Lock writeLock;

        public Locker() {
            // TODO: We should also use file lock (though it is not a big issue since
            //       NB cannot run concurrently with itself anyway).

            ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
            this.readLock = readWriteLock.readLock();
            this.writeLock = readWriteLock.writeLock();
        }

        public <R> R doRead(IoTask<? extends R> task) throws IOException {
            readLock.lock();
            try {
                return task.run();
            } finally {
                readLock.unlock();
            }
        }

        public <R> R doWrite(IoTask<? extends R> task) throws IOException {
            writeLock.lock();
            try {
                return task.run();
            } finally {
                writeLock.unlock();
            }
        }
    }

    private interface IoTask<R> {
        public R run() throws IOException;
    }
}
