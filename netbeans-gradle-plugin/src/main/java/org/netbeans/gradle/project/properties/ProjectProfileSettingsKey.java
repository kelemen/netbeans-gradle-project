package org.netbeans.gradle.project.properties;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.properties.global.GlobalProfileSettingsKey;

public final class ProjectProfileSettingsKey implements ProfileSettingsKey {
    private final Path projectDir;
    private final ProfileKey key;

    private ProjectProfileSettingsKey(@Nonnull Path projectDir, @Nullable ProfileKey key) {
        this.projectDir = projectDir;
        this.key = key;
    }

    public static ProfileSettingsKey getForProject(@Nonnull NbGradleProject project, @Nullable ProfileKey key) {
        return getForProject(project.currentModel().getValue().getSettingsDir(), key);
    }

    public static ProfileSettingsKey getForProject(@Nonnull Path projectDir, @Nullable ProfileKey key) {
        Objects.requireNonNull(projectDir, "projectDir");

        if (ProfileKey.GLOBAL_PROFILE.equals(key)) {
            return GlobalProfileSettingsKey.GLOBAL_DEFAULTS_KEY;
        }
        return new ProjectProfileSettingsKey(projectDir, key);
    }

    @Nonnull
    private ProjectProfileSettingsKey getDefaultProfile() {
        return key != null
                ? new ProjectProfileSettingsKey(projectDir, null)
                : this;
    }

    @Nonnull
    @Override
    public List<ProfileSettingsKey> getWithFallbacks() {
        if (key == null) {
            return Arrays.<ProfileSettingsKey>asList(this, GlobalProfileSettingsKey.GLOBAL_DEFAULTS_KEY);
        }
        else {
            return Arrays.<ProfileSettingsKey>asList(
                    this,
                    getDefaultProfile(),
                    GlobalProfileSettingsKey.GLOBAL_DEFAULTS_KEY);
        }
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
    public LoadableSingleProfileSettingsEx openUnloadedProfileSettings() {
        return new ProjectProfileSettings(this);
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

        final ProjectProfileSettingsKey other = (ProjectProfileSettingsKey)obj;
        return Objects.equals(this.projectDir, other.projectDir)
                && Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        return "ProjectProfileSettingsKey{" + "projectDir=" + projectDir + ", key=" + key + '}';
    }
}
