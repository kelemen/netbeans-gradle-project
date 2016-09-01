package org.netbeans.gradle.project.properties;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;

public final class ProfileFileDef {
    private final Path profileFile;
    private final ConfigSaveOptions saveOptions;

    public ProfileFileDef(Path profileFile, ConfigSaveOptions saveOptions) {
        ExceptionHelper.checkNotNullArgument(profileFile, "profileFile");
        ExceptionHelper.checkNotNullArgument(saveOptions, "saveOptions");

        this.profileFile = profileFile;
        this.saveOptions = saveOptions;
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
