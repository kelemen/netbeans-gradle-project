package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.event.OneShotChangeListenerManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;

final class ProjectProfileSettings implements SingleProfileSettingsEx {
    private static final Logger LOGGER = Logger.getLogger(ProjectProfileSettings.class.getName());
    // Should be single threaded to avoid unnecessary multiple load.
    private static final TaskExecutorService SAVE_LOAD_EXECUTOR
            = NbTaskExecutors.newExecutor("Profile-I/O", 1);

    private final ProfileSettingsKey key;
    private final ProfileSettings settings;

    private final Lock ioLock;
    private volatile boolean loadedOnce;

    private volatile boolean dirty;

    private final OneShotChangeListenerManager loadedListeners;

    private final UpdateTaskExecutor loadExecutor;
    private final UpdateTaskExecutor saveExecutor;

    public ProjectProfileSettings(ProfileSettingsKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        this.key = key;
        this.settings = new ProfileSettings();
        this.ioLock = new ReentrantLock();
        this.dirty = false;
        this.loadedOnce = false;
        this.loadedListeners = OneShotChangeListenerManager.getSwingNotifier();
        this.saveExecutor = new GenericUpdateTaskExecutor(SAVE_LOAD_EXECUTOR);
        this.loadExecutor = new GenericUpdateTaskExecutor(SAVE_LOAD_EXECUTOR);
    }

    @Override
    public ProfileKey getKey() {
        return key.getKey();
    }

    private Project tryGetProject() throws IOException {
        Path projectDir = key.getProjectDir();
        FileObject projectDirObj = FileUtil.toFileObject(projectDir.toFile());

        return projectDirObj != null
                ? ProjectManager.getDefault().findProject(projectDirObj)
                : null;
    }

    private Path tryGetProfileFile() {
        return SettingsFiles.getProfileFile(key.getProjectDir(), key.getKey());
    }

    public ListenerRef notifyWhenLoaded(Runnable onLoaded) {
        ensureLoaded();
        return loadedListeners.registerOrNotifyListener(onLoaded);
    }

    public void ensureLoaded() {
        if (loadedOnce) {
            return;
        }

        loadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                loadNowIfNotLoaded();
            }
        });
    }

    public void ensureLoadedAndWait() {
        loadNowIfNotLoaded();
    }

    public void loadEventually() {
        loadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                loadNowAlways();
            }
        });
    }

    public void loadAndWait() {
        loadNowAlways();
    }

    private void loadNowIfNotLoaded() {
        if (!loadedOnce) {
            loadNow(true);
        }
    }

    private void loadNowAlways() {
        loadNow(false);
    }

    private void loadNow(boolean skipIfLoaded) {
        try {
            loadNowUnsafe(skipIfLoaded);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void loadNowUnsafe(boolean skipIfLoaded) throws IOException {
        try {
            Path profileFile = tryGetProfileFile();
            if (profileFile == null) {
                LOGGER.log(Level.WARNING, "Cannot find location to save the profile: {0}", key);
                return;
            }

            loadFromFile(profileFile, skipIfLoaded);
        } finally {
            loadedListeners.fireEventually();
        }
    }

    private void loadFromFile(Path profileFile, boolean skipIfLoaded) {
        ioLock.lock();
        try {
            if (!skipIfLoaded || !loadedOnce) {
                settings.loadFromFile(profileFile);
            }
        } finally {
            loadedOnce = true;
            ioLock.unlock();
        }
    }

    private void saveEventually() {
        if (!dirty) {
            return;
        }

        saveExecutor.execute(new Runnable() {
            @Override
            public void run() {
                saveAndWait();
            }
        });
    }

    @Override
    public void saveAndWait() {
        try {
            saveNowUnsafe();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to save the properties.", ex);
        }
    }

    private void saveNowUnsafe() throws IOException {
        Project project = tryGetProject();
        if (project == null) {
            LOGGER.log(Level.WARNING, "No project in {0}", key.getProjectDir());
            return;
        }

        Path profileFile = tryGetProfileFile();
        if (profileFile == null) {
            return;
        }

        ConfigSaveOptions saveOptions = ConfigXmlUtils.getSaveOptions(project, profileFile);

        savetoFile(profileFile, saveOptions);
    }

    private void savetoFile(Path profileFile, ConfigSaveOptions saveOptions) throws IOException {
        ioLock.lock();
        try {
            if (dirty) {
                dirty = false;
                settings.saveToFile(profileFile, saveOptions);
            }
        } finally {
            ioLock.unlock();
        }
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        return settings.getAuxConfigValue(key);
    }

    @Override
    public boolean setAuxConfigValue(DomElementKey key, Element value) {
        boolean result = settings.setAuxConfigValue(key, value);
        dirty = true;
        saveEventually();
        return result;
    }

    @Override
    public <ValueType> MutableProperty<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
        final MutableProperty<ValueType> result = settings.getProperty(propertyDef);
        return new MutableProperty<ValueType>() {
            @Override
            public void setValue(ValueType value) {
                result.setValue(value);
                dirty = true;
                saveEventually();
            }

            @Override
            public ValueType getValue() {
                return result.getValue();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }
        };
    }

    @Override
    public Collection<DomElementKey> getAuxConfigKeys() {
        return settings.getAuxConfigKeys();
    }
}
