package org.netbeans.gradle.project.api.query;

import java.net.URL;
import java.util.List;
import org.netbeans.gradle.project.CollectionUtils;

/**
 * Defines a platform used by a Gradle project. Currently, the platform is only
 * used to determine the boot classpath of source files. Other optional
 * properties might be added in the future.
 * <P>
 * This class is immutable and as such is safe to be accessed by multiple
 * concurrent threads.
 *
 * @see GradleProjectPlatformQuery
 * @see ProjectPlatformRef
 */
public final class ProjectPlatform {
    private final String displayName;
    private final String name;
    private final String version;
    private final List<URL> bootLibraries;

    /**
     * Creates a new {@code ProjectPlatform} initialized with the mandatory
     * properties.
     *
     * @param displayName the user friendly name of the platform to be displayed
     *   in a GUI. This argument cannot be {@code null}.
     * @param name the name of the platform through which the platform can be
     *   looked up (e.g.: "j2se", "android"). This name is case-sensitive and
     *   cannot be {@code null}.
     * @param version the version of the platform. The exact format of the
     *   version might vary from platform to platform. This argument cannot be
     *   {@code null}.
     * @param bootLibraries the libraries to be added to the boot classpath
     *   of source files (e.g.: jars for a Java project). This argument cannot
     *   be {@code null} and cannot contain {@code null} elements.
     */
    public ProjectPlatform(
            String displayName,
            String name,
            String version,
            List<URL> bootLibraries) {
        if (displayName == null) throw new NullPointerException("displayName");
        if (name == null) throw new NullPointerException("name");
        if (version == null) throw new NullPointerException("version");

        this.displayName = displayName;
        this.name = name;
        this.version = version;
        this.bootLibraries = CollectionUtils.copyNullSafeList(bootLibraries);
    }

    /**
     * Returns the libraries to be added to the boot classpath of source files
     * as specified at construction time.
     * <P>
     * For a Java project, the URLs might be links to jar files on the local
     * harddrive.
     *
     * @return the libraries to be added to the boot classpath of source files.
     *   This method never returns {@code null} and the returned list does not
     *   contain {@code null} elements.
     */
    public List<URL> getBootLibraries() {
        return bootLibraries;
    }

    /**
     * Returns the user friendly name of the platform which might be displayed
     * to the user on a GUI.
     *
     * @return the user friendly name of the platform which might be displayed
     *   to the user on a GUI. This method never returns {@code null}.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the programmatic name of this platform. This name can be used to
     * look up the platform. This string is not meant to be displayed to
     * the user but to be saved in a property file to be able to later find the
     * platform again.
     *
     * @return the programmatic name of this platform. This method never returns
     *   {@code null}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version of this platform. Along with the
     * {@link #getName() name} of this platform, the version unambiguously
     * defines the platform. The exact format of the version might vary from
     * platform to platform.
     *
     * @return the version of this platform. This method never returns
     *   {@code null}.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the same string as {@link #getDisplayName()}.
     *
     * @return the same string as {@link #getDisplayName()}. This method never
     *   returns {@code null}.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
