package org.netbeans.gradle.project.properties2;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.SettingsFiles;

public final class ProfileSettingsKey {
    private final Path projectDir;
    private final ProfileKey key;

    public ProfileSettingsKey(@Nonnull Path projectDir, @Nullable ProfileKey key) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");

        this.projectDir = projectDir;
        this.key = key;
    }

    @Nonnull
    public ProfileSettingsKey getDefaultProfile() {
        return key != null
                ? new ProfileSettingsKey(projectDir, null)
                : this;
    }

    @Nonnull
    public List<ProfileSettingsKey> getWithFallbacks() {
        if (key == null) {
            return Collections.singletonList(this);
        }
        else {
            return Arrays.asList(this, getDefaultProfile());
        }
    }

    @Nonnull
    public Path getProfileFile() {
        return SettingsFiles.getProfileFile(projectDir, key);
    }

    @Nonnull
    public Path getProjectDir() {
        return projectDir;
    }

    @Nullable
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
