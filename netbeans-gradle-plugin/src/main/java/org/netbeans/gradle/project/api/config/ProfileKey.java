package org.netbeans.gradle.project.api.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a unique ID of a profile within a particular project. That is, this
 * ID does not need to be unique between two different projects.
 * <P>
 * Instances of this class are immutable and as such can be shared without any
 * further synchronization.
 */
public final class ProfileKey {
    /**
     * Defines the key for the default profile where a method takes a
     * {@code ProfileKey}.
     * <P>
     * <B>Note</B>: The value of this field is simply {@code null}, so you
     * cannot call any of its methods.
     */
    public static final ProfileKey DEFAULT_PROFILE = null;

    private static final String PRIVATE_PROFILE_GROUP = "private";

    /**
     * Defines the key to access the default user private profile. It is rarely
     * appropriate to access this profile, instead you can use the group name
     * {@literal "private"} for your profile to be considered user specific.
     */
    public static final ProfileKey PRIVATE_PROFILE = new ProfileKey(PRIVATE_PROFILE_GROUP, "aux-config");

    /**
     * Defines the key to access the global profile.
     */
    public static final ProfileKey GLOBAL_PROFILE = new ProfileKey("*", "project-defaults");

    private final String groupName;
    private final String fileName;

    /**
     * Creates a new {@code ProfileKey} instance with the specified properties.
     *
     * @param groupName the group (or namespace) of the profile. This argument
     *   must be a valid directory name or {@code null}. A {@code null} group
     *   means the default group where users create their profile.
     * @param fileName the name of the file into which the profile must be
     *   saved. The filename should not contain a directory path. This argument
     *   cannot be {@code null}.
     */
    public ProfileKey(@Nullable String groupName, @Nonnull String fileName) {
        this.groupName = groupName;
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }

    /**
     * Converts a {@code ProfileDef} to a {@code ProfileKey}. That is,
     * this method returns {@code profileDef.getProfileKey()} if the given
     * argument is not {@code null} and {@code null} if the passed argument is
     * {@code null}.
     *
     * @param profileDef the {@code ProfileDef} whose {@code ProfileKey} is to
     *   be retrieved. This argument can be {@code null}, in which case the
     *   return value is also {@code null}.
     * @return the {@code ProfileKey} representing the given {@code ProfileDef}.
     *   This method only returns {@code null} if the passed {@code ProfileDef}
     *   is {@code null}.
     */
    @Nullable
    public static ProfileKey fromProfileDef(@Nullable ProfileDef profileDef) {
        return profileDef != null ? profileDef.getProfileKey() : null;
    }

    /**
     * Returns {@code true} if the given profile might be shared across
     * different machines. That is, if a profile is shared it is not recommended
     * to save settings in a manner which is machine specific. For example:
     * saving absolute paths pointing into the user's file system.
     *
     * @param profileKey the key identifying the profile. This argument
     *   can be {@code null}, which refers to the default profile.
     * @return {@code true} if the given profile might be shared across
     *   different machines, {@code false} otherwise
     */
    public static boolean isSharedProfile(@Nullable ProfileKey profileKey) {
        return !isGlobal(profileKey) && !isPrivate(profileKey);
    }

    /**
     * Returns {@code true} if the given profile refers to the profile which
     * is global to the IDE instance.
     *
     * @param profileKey the key identifying the profile. This argument
     *   can be {@code null}, which refers to the default profile.
     * @return {@code true} if the given profile refers to the profile which
     *   is global to the IDE instance, {@code false} otherwise
     */
    public static boolean isGlobal(@Nullable ProfileKey profileKey) {
        return Objects.equals(profileKey, ProfileKey.GLOBAL_PROFILE);
    }

    /**
     * Returns {@code true} if the given profile is project specific but is
     * explicitly defined to be unsharable across machines.
     *
     * @param profileKey the key identifying the profile. This argument
     *   can be {@code null}, which refers to the default profile.
     * @return {@code true} if the given profile is project specific but is
     *   explicitly defined to be unsharable across machines, {@code false}
     *   otherwise
     */
    public static boolean isPrivate(@Nullable ProfileKey profileKey) {
        return profileKey != null
                && PRIVATE_PROFILE_GROUP.equals(profileKey.getGroupName());
    }

    /**
     * Returns the group (namespace) of this profile. The group name must be a
     * valid directory name.
     *
     * @return the group (namespace) of this profile. This method may return
     *   {@code null} which means the default namespace into which users create
     *   their own profiles.
     */
    @Nullable
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the filename of this profile where the profile is to be stored.
     * This path should not contain a directory path but a simple filename.
     *
     * @return the filename of this profile where the profile is to be stored.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public String getFileName() {
        return fileName;
    }

    /**
     * {@inheritDoc }
     *
     * @return a hash code compatible with the {@code equals} method
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.groupName);
        hash = 29 * hash + Objects.hashCode(this.fileName);
        return hash;
    }

    /**
     * Checks whether the given object is a {@code ProfileKey} defining the
     * same profile. Two {@code ProfileKey} instances are equal, if and only, if
     * both their {@link #getGroupName() group} and {@link #getFileName() filename}
     * properties are equal.
     * <P>
     * Note that although the check for equality is case-sensitive, you should
     * avoid using filenames (or groups) different only by case for different
     * profiles.
     *
     * @param obj the object to be compared against this profile. This argument
     *   can be {@code null}, in which case the return value is {@code null}.
     * @return {@code true} if the specified object defines the same profile as
     *   this {@code ProfileKey}, {@code false} otherwise
     */
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
