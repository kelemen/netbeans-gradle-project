package org.netbeans.gradle.project.api.config;

/**
 * Defines a specific profile (configuration) for a Gradle project. Note that
 * the projects of the same multi-project build always share their
 * configuration.
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
 */
public final class ProfileDef {
    private final String groupName;
    private final String fileName;
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
    public ProfileDef(String groupName, String fileName, String displayName) {
        if (fileName == null) throw new NullPointerException("fileName");
        if (displayName == null) throw new NullPointerException("displayName");

        this.groupName = groupName;
        this.fileName = fileName;
        this.displayName = displayName;
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
        return groupName;
    }

    /**
     * Returns the filename of this profile where the profile is to be stored.
     * This path should not contain a directory path but a simple filename.
     *
     * @return the filename of this profile where the profile is to be stored.
     *   This method never returns {@code null}.
     */
    public String getFileName() {
        return fileName;
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
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (groupName != null ? groupName.hashCode() : 0);
        hash = 67 * hash + fileName.hashCode();
        return hash;
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
        if ((this.groupName == null) ? (other.groupName != null) : !this.groupName.equals(other.groupName)) {
            return false;
        }

        return this.fileName.equals(other.fileName);
    }
}
