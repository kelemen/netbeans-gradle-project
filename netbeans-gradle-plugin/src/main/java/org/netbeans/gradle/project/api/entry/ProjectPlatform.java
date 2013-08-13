package org.netbeans.gradle.project.api.entry;

import java.net.URL;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.openide.filesystems.FileObject;

/**
 * Defines a platform used by a Gradle project. Currently, the platform is only
 * used to determine the boot classpath of source files. Other optional
 * properties might be added in the future.
 * <P>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently. Implementations are recommended to be immutable as
 * well.
 *
 * @see GradleProjectPlatformQuery
 */
public interface ProjectPlatform {
    /**
     * Returns the root folder containing the platform or {@code null} if it is
     * unknown. This value might be used to passed for Gradle commands.
     *
     * @return the root folder containing the platform or {@code null} if it is
     *   unknown
     */
    @Nullable
    public FileObject getRootFolder();

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
    @Nonnull
    public Collection<URL> getBootLibraries();

    /**
     * Returns the source of the libraries of this platform if available.
     *
     * @return the source of the libraries of this platform if available.
     *   This method never returns {@code null}, if the sources are unavailable,
     *   the return value is an empty list.
     */
    @Nonnull
    public Collection<URL> getSourcePaths();

    /**
     * Returns the user friendly name of the platform which might be displayed
     * to the user on a GUI.
     *
     * @return the user friendly name of the platform which might be displayed
     *   to the user on a GUI. This method never returns {@code null}.
     */
    @Nonnull
    public String getDisplayName() ;

    /**
     * Returns the programmatic name of this platform. This name can be used to
     * look up the platform. This string is not meant to be displayed to
     * the user but to be saved in a property file to be able to later find the
     * platform again.
     *
     * @return the programmatic name of this platform. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public String getName();

    /**
     * Returns the version of this platform. Along with the
     * {@link #getName() name} of this platform, the version unambiguously
     * defines the platform. The exact format of the version might vary from
     * platform to platform.
     *
     * @return the version of this platform. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public String getVersion();

    /**
     * Returns {@code true} if this platform is equivalent to the the given
     * platform, {@code false} otherwise.
     * <P>
     * Note that this method must conform to the generic specification of
     * {@link Object#equals(Object) equals}.
     *
     * @param obj the other platform to which this platform is compared to. This
     *   argument can be {@code null}, in which case the return value is
     *   {@code null}.
     * @return {@code true} if this platform is equivalent to the the given
     *   platform, {@code false} otherwise
     *
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object obj);

    /**
     * {@inheritDoc }
     *
     * @return the hash code of this platform
     */
    @Override
    public int hashCode();
}
