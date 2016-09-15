package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.config.ProfileKey;

public class ProfileInfo {
    private final ProfileKey profileKey;
    private final String displayName;

    public ProfileInfo(ProfileKey profileKey, String displayName) {
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");

        this.profileKey = profileKey;
        this.displayName = displayName;
    }

    @Nullable
    public ProfileKey getProfileKey() {
        return profileKey;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }
}
