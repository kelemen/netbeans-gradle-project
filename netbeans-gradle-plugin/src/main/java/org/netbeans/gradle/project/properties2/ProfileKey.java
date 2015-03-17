package org.netbeans.gradle.project.properties2;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.config.ProfileDef;

public final class ProfileKey {
    public static final ProfileKey DEFAULT_PROFILE = null;
    public static final ProfileKey PRIVATE_PROFILE = new ProfileKey("private", "aux-config");

    private final String groupName;
    private final String fileName;

    public ProfileKey(@Nonnull ProfileDef profileDef) {
        this(profileDef.getGroupName(), profileDef.getFileName());
    }

    public ProfileKey(@Nullable String groupName, @Nonnull String fileName) {
        ExceptionHelper.checkNotNullArgument(fileName, "fileName");

        this.groupName = groupName;
        this.fileName = fileName;
    }

    public static ProfileKey fromProfileDef(ProfileDef profileDef) {
        return profileDef != null ? new ProfileKey(profileDef) : null;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.groupName);
        hash = 29 * hash + Objects.hashCode(this.fileName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ProfileKey other = (ProfileKey)obj;
        return Objects.equals(this.groupName, other.groupName)
                && Objects.equals(this.fileName, other.fileName);
    }
}
