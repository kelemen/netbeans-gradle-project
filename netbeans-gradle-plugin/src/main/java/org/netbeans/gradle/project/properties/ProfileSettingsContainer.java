package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.utils.ExceptionHelper;

public final class ProfileSettingsContainer {
    private static final AtomicReference<ProfileSettingsContainer> DEFAULT_REF = new AtomicReference<>(null);

    private final Lock mainLock;
    private final WeakValueHashMap<ProfileSettingsKey, ProjectProfileSettings> loaded;

    private ProfileSettingsContainer() {
        this.mainLock = new ReentrantLock();
        this.loaded = new WeakValueHashMap<>();
    }

    public static ProfileSettingsContainer getDefault() {
        ProfileSettingsContainer result = DEFAULT_REF.get();
        if (result == null) {
            result = new ProfileSettingsContainer();
            if (DEFAULT_REF.compareAndSet(null, result)) {
                final ProfileSettingsContainer toPersistBeforeTerminate = result;
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        toPersistBeforeTerminate.saveAllProfilesNow();
                    }
                }));
            }
            else {
                result = DEFAULT_REF.get();
            }
        }
        return result;
    }

    private void saveAllProfilesNow() {
        List<ProjectProfileSettings> toSave;
        mainLock.lock();
        try {
            toSave = new ArrayList<>(loaded.values());
        } finally {
            mainLock.unlock();
        }

        for (ProjectProfileSettings settings: toSave) {
            settings.saveAndWait();
        }
    }

    public ProjectProfileSettings getProfileSettings(ProfileSettingsKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        ProjectProfileSettings result;
        boolean loadNow = false;

        mainLock.lock();
        try {
            result = loaded.get(key);
            if (result == null) {
                result = new ProjectProfileSettings(key);
                loadNow = true;
                loaded.put(key, result);
            }
        } finally {
            mainLock.unlock();
        }

        if (loadNow) {
            result.ensureLoaded();
        }
        return result;
    }

    public List<ProjectProfileSettings> getAllProfileSettings(Collection<ProfileSettingsKey> keys) {
        ExceptionHelper.checkNotNullElements(keys, "keys");

        List<ProjectProfileSettings> result = new ArrayList<>(keys.size());
        for (ProfileSettingsKey key: keys) {
            result.add(getProfileSettings(key));
        }
        return result;
    }
}
