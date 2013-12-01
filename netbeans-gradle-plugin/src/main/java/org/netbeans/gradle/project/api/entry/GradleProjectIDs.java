package org.netbeans.gradle.project.api.entry;

/**
 * Defines constants important for identification of the Gradle plugin.
 */
public final class GradleProjectIDs {
    /**
     * The name of the Gradle plugin as defined in the manifest (OpenIDE-Module):
     * "org.netbeans.gradle.project".
     * <P>
     * This string can be used for {@link org.netbeans.spi.project.ProjectServiceProvider}
     * to add entries to the lookup of Gradle projects. Note however, that the
     * preferred way to add entries to the project lookup is to return it in
     * the {@link GradleProjectExtension2#getPermanentProjectLookup()} method.
     */
    public static final String MODULE_NAME = "org.netbeans.gradle.project";

    private GradleProjectIDs() {
        throw new AssertionError();
    }
}
