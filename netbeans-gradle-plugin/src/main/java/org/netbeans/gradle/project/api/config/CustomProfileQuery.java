package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;

/**
 * Defines a query which might return custom profile names always available for
 * a project.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see ProfileDef
 */
public interface CustomProfileQuery {
    /**
     * Returns the profiles always available for the project using the
     * associated extension. The order of the returned profiles does not matter
     * because profiles are kept sorted.
     * <P>
     * <B>Important note</B>: The return profiles are highly recommended to
     * have a non-null {@link ProfileDef#getGroupName() group} defined.
     * <P>
     * The returned list of profiles are only expected to change after project
     * (re)load.
     *
     * @return the profiles always available for the project using the
     *   associated extension. This method may never return {@code null}.
     */
    @Nonnull
    public Iterable<ProfileDef> getCustomProfiles();
}
