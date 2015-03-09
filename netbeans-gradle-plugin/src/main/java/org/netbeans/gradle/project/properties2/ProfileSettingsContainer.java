package org.netbeans.gradle.project.properties2;

import java.io.IOException;
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

    public ProjectProfileSettings getLoadedProfileSettings(ProfileSettingsKey key) throws IOException {
        ProjectProfileSettings result = getProfileSettings(key);
        result.ensureLoaded();
        return result;
    }

    public ProjectProfileSettings getProfileSettings(ProfileSettingsKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        mainLock.lock();
        try {
            ProjectProfileSettings result = loaded.get(key);
            if (result == null) {
                result = new ProjectProfileSettings(key);
                loaded.put(key, result);
            }
            return result;
        } finally {
            mainLock.unlock();
        }
    }
}
