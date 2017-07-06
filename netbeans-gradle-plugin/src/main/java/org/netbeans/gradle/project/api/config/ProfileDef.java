package org.netbeans.gradle.project.api.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a specific profile (configuration) for a Gradle project. Note that
 * the projects of the same multi-project build always share their
 * configuration. Extensions may also define they own profiles via the
 * {@link CustomProfileQuery} query.
 * <P>
 * The following properties define a profile:
 * <ul>
 *  <li>
 *   {@link #getGroupName() Group}: This property is provided, so that
 *   extensions might put their configuration into different groups lessening
 *   the likelihood of name collision. The group name must be a valid directory
 *   name. This property might be {@code null} for the default namespace. Note
 *   however that users create profiles into the default namespace, so
 *   extensions should not specify their profiles in the default namespace.
 *  </li>
 *  <li>{@link #getFileName() Filename}: The name of the file storing the
 *   configuration. This property should only contain the filename and no
 *   directory. Note that you should not use filenames different only by case
 *   for different profiles.
 *  </li>
 *  <li>
 *   {@link #getDisplayName() Display name}: The name to be displayed to the
 *   user on the GUI.
 *  </li>
 * </ul>
 * <P>
 * The {@link #equals(Object) equals} and {@link #hashCode() hashCode} methods
 * define two {@code ProfileDef} instances to be equivalent, if, and only, if
 * they have the same group and filename.
 *
 * @see CustomProfileQuery
 */
public final class ProfileDef {
    private final ProfileKey profileKey;
    private final String displayName;

    /**
     * Creates a new {@code ProfileDef} instance with the specified properties.
     *
     * @param groupName the group (or namespace) of the profile. This argument
     *   must be a valid directory name or {@code null}. A {@code null} group
     *   means the default group where users create their profile.
     * @param fileName the name of the file into which the profile must be
     *   saved. The filename should not contain a directory path. This argument
     *   cannot be {@code null}.
     * @param displayName the name to be displayed to the user on the GUI. This
     *   argument cannot be {@code null}.
     */
    public ProfileDef(
            @Nullable String groupName,
            @Nonnull String fileName,
            @Nonnull String displayName) {
        this(new ProfileKey(groupName, fileName), displayName);
    }

    /**
     * Creates a new {@code ProfileDef} instance with the specified properties.
     *
     * @param profileKey the key identifying the profile within a particular
     *   project. This argument cannot be {@code null}.
     * @param displayName the name to be displayed to the user on the GUI. This
     *   argument cannot be {@code null}.
     */
    public ProfileDef(
            @Nonnull ProfileKey profileKey,
            @Nonnull String displayName) {
        this.profileKey = Objects.requireNonNull(profileKey, "profileKey");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    /**
     * Returns the key identifying the profile within a particular project.
     *
     * @return the key identifying the profile within a particular project.
     *   This method never returns {@code null}.
     */
    public ProfileKey getProfileKey() {
        return profileKey;
    }

    /**
     * Returns the group (namespace) of this profile. The group name must be a
     * valid directory name.
     *
     * @return the group (namespace) of this profile. This method may return
     *   {@code null} which means the default namespace into which users create
     *   their own profiles.
     */
    public String getGroupName() {
        return profileKey.getGroupName();
    }

    /**
     * Returns the filename of this profile where the profile is to be stored.
     * This path should not contain a directory path but a simple filename.
     *
     * @return the filename of this profile where the profile is to be stored.
     *   This method never returns {@code null}.
     */
    public String getFileName() {
        return profileKey.getFileName();
    }

    /**
     * Returns the name of this profile as displayed users on the GUI.
     *
     * @return the name of this profile as displayed users on the GUI. This
     *   method never returns {@code null}.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * {@inheritDoc }
     *
     * @return a hash code compatible with the {@code equals} method
     */
    @Override
    public int hashCode() {
        return 469 + profileKey.hashCode();
    }

    /**
     * Checks whether the given object is a {@code ProfileDef} defining the
     * same profile. The equality ignores the display name and only considers
     * the {@link #getGroupName() group} and the {@link #getFileName() filename}.
     * <P>
     * Note that although the check for equality is case-sensitive, you should
     * avoid using filenames (or groups) different only by case for different
     * profiles.
     *
     * @param obj the object to be compared against this profile. This argument
     *   can be {@code null}, in which case the return value is {@code null}.
     * @return {@code true} if the specified object defines the same profile as
     *   this {@code ProfileDef}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ProfileDef other = (ProfileDef)obj;
        return this.profileKey.equals(other.profileKey);
    }
}
