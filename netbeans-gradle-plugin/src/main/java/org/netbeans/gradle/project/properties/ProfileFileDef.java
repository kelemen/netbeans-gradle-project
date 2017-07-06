package org.netbeans.gradle.project.properties;

import java.nio.file.Path;
import java.util.Objects;

public final class ProfileFileDef {
    private final Path profileFile;
    private final ConfigSaveOptions saveOptions;

    public ProfileFileDef(Path profileFile, ConfigSaveOptions saveOptions) {
        this.profileFile = Objects.requireNonNull(profileFile, "profileFile");
        this.saveOptions = Objects.requireNonNull(saveOptions, "saveOptions");
    }

    public Path getProfileFile() {
        return profileFile;
    }

    public ConfigSaveOptions getSaveOptions() {
        return saveOptions;
    }

    @Override
    public String toString() {
        return "ProfileFileDef{" + profileFile + '}';
    }
}
