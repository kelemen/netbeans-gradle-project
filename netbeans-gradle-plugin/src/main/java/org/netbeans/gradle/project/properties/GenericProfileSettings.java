package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.event.OneShotChangeListenerManager;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.w3c.dom.Element;

public final class GenericProfileSettings implements LoadableSingleProfileSettingsEx {
    private static final Logger LOGGER = Logger.getLogger(ProjectProfileSettings.class.getName());
    // Should be single threaded to avoid unnecessary multiple load.
    private static final TaskExecutorService SAVE_LOAD_EXECUTOR
            = NbTaskExecutors.newExecutor("Profile-I/O", 1);

    private final ProfileLocationProvider locationProvider;
    private final ProfileSettings settings;

    private final Lock ioLock;
    private volatile boolean loadedOnce;

    private volatile boolean dirty;

    private final OneShotChangeListenerManager loadedListeners;

    private final UpdateTaskExecutor loadExecutor;
    private final UpdateTaskExecutor saveExecutor;

    public GenericProfileSettings(ProfileLocationProvider locationProvider) {
        this.locationProvider = Objects.requireNonNull(locationProvider, "locationProvider");
        this.settings = new ProfileSettings();
        this.ioLock = new ReentrantLock();
        this.dirty = false;
        this.loadedOnce = false;
        this.loadedListeners = OneShotChangeListenerManager.getSwingNotifier();
        this.saveExecutor = new GenericUpdateTaskExecutor(SAVE_LOAD_EXECUTOR);
        this.loadExecutor = new GenericUpdateTaskExecutor(SAVE_LOAD_EXECUTOR);
    }

    public static GenericProfileSettings createTestMemorySettings() {
        return new GenericProfileSettings(new ProfileLocationProvider() {
            @Override
            public ProfileKey getKey() {
                return new ProfileKey("?", "test");
            }

            @Override
            public Path tryGetOutputPath() throws IOException {
                return null;
            }

            @Override
            public ProfileFileDef tryGetOutputDef() throws IOException {
                return null;
            }
        });
    }

    public void clearSettings() {
        settings.clearSettings();
    }

    public ConfigTree getContentSnapshot() {
        return settings.getContentSnapshot();
    }

    @Override
    public ProfileKey getKey() {
        return locationProvider.getKey();
    }

    @Override
    public ListenerRef notifyWhenLoaded(Runnable onLoaded) {
        ensureLoaded();
        return loadedListeners.registerOrNotifyListener(onLoaded);
    }

    @Override
    public void ensureLoaded() {
        if (loadedOnce) {
            return;
        }

        loadExecutor.execute(this::loadNowIfNotLoaded);
    }

    @Override
    public void ensureLoadedAndWait() {
        loadNowIfNotLoaded();
    }

    public void loadEventually() {
        loadExecutor.execute(this::loadNowAlways);
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
            Path profileFile = locationProvider.tryGetOutputPath();
            if (profileFile == null) {
                LOGGER.log(Level.WARNING, "Cannot find location to save the profile: {0}", getKey());
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

        saveExecutor.execute(this::saveAndWait);
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
        ProfileFileDef outputDef = locationProvider.tryGetOutputDef();
        if (outputDef != null) {
            savetoFile(outputDef.getProfileFile(), outputDef.getSaveOptions());
        }
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
