package org.netbeans.gradle.project.api.config.ui;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.project.api.config.ProfileKey;

/**
 * Defines basic information about a profile to be listed. Istances of this class
 * are immutable and as such, safely shareable across multiple threads concurrently.
 *
 * @see ProfileEditorFactory
 */
public final class ProfileInfo {
    private final ProfileKey profileKey;
    private final String displayName;

    /**
     * Creates a new {@code ProfileInfo} with the given basic information.
     *
     * @param profileKey the key identifying the profile. This key is project independent
     *   and must be resolved against a particular project. This argument can be
     *   {@code null}, which refers to the default profile.
     * @param displayName the name of the profile associated with the given key in a
     *   format displayable to a user. This argument cannot be {@code null}.
     */
    public ProfileInfo(@Nullable ProfileKey profileKey, @Nonnull String displayName) {
        this.profileKey = profileKey;
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    /**
     * Returns the key identifying the profile associated with this information.
     * The key is project independent and must be resolved against a particular project.
     *
     * @return he key identifying the profile associated with this information.
     *   This method may return {@code null}, if the key refers to the default
     *   profile.
     */
    @Nullable
    public ProfileKey getProfileKey() {
        return profileKey;
    }

    /**
     * Returns the name of the associated profile in a format displayable to the user.
     *
     * @return the name of the associated profile in a format displayable to the user.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns {@code true} if the associated profile might be shared across
     * different machines. That is, if a profile is shared it is not recommended
     * to save settings in a manner which is machine specific. For example:
     * saving absolute paths pointing into the user's file system.
     *
     * @return {@code true} if the associated profile might be shared across
     *   different machines, {@code false} otherwise
     */
    public boolean isSharedProfile() {
        return ProfileKey.isSharedProfile(profileKey);
    }

    /**
     * Returns {@code true} if the associated profile refers to the profile which
     * is global to the IDE instance.
     *
     * @return {@code true} if the associated profile refers to the profile which
     *   is global to the IDE instance, {@code false} otherwise
     */
    public boolean isGlobal() {
        return ProfileKey.isGlobal(profileKey);
    }

    /**
     * Returns {@code true} if the associated profile is project specific but is
     * explicitly defined to be unsharable across machines.
     *
     * @return {@code true} if the associated profile is project specific but is
     *   explicitly defined to be unsharable across machines, {@code false}
     *   otherwise
     */
    public boolean isPrivate() {
        return ProfileKey.isPrivate(profileKey);
    }
}
