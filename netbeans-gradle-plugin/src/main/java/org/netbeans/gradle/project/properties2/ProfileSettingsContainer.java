package org.netbeans.gradle.project.properties2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.WeakValueHashMap;

public final class ProfileSettingsContainer {
    private static final ProfileSettingsContainer DEFAULT = new ProfileSettingsContainer();

    private final Lock mainLock;
    private final WeakValueHashMap<ProfileSettingsKey, ProjectProfileSettings> loaded;

    private ProfileSettingsContainer() {
        this.mainLock = new ReentrantLock();
        this.loaded = new WeakValueHashMap<>();
    }

    public static ProfileSettingsContainer getDefault() {
        return DEFAULT;
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
        List<ProjectProfileSettings> result = new ArrayList<>(keys.size());
        for (ProfileSettingsKey key: keys) {
            result.add(getProfileSettings(key));
        }
        return result;
    }
}
