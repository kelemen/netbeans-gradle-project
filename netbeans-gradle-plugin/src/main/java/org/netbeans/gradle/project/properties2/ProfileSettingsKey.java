package org.netbeans.gradle.project.properties2;

import java.nio.file.Path;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class ProfileSettingsKey {
    private final Path projectDir;
    private final ProfileKey key;

    public ProfileSettingsKey(Path projectDir, ProfileKey key) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
        ExceptionHelper.checkNotNullArgument(key, "key");

        this.projectDir = projectDir;
        this.key = key;
    }

    public Path getProjectDir() {
        return projectDir;
    }

    public ProfileKey getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.projectDir);
        hash = 19 * hash + Objects.hashCode(this.key);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ProfileSettingsKey other = (ProfileSettingsKey)obj;
        return Objects.equals(this.projectDir, other.projectDir)
                && Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        return "ProfileSettingsKey{" + "projectDir=" + projectDir + ", key=" + key + '}';
    }
}
