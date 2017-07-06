package org.netbeans.gradle.project.api.entry;

import java.net.URI;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jtrim2.event.ListenerRef;

/**
 * Defines a query which can find a platform for a Gradle project. The platform
 * can be looked up {@link #tryFindPlatformByName(String, String) by name} or
 * {@link #tryFindPlatformByUri(URI) by an URI}.
 * <P>
 * Instances of {@code GradleProjectPlatformQuery} are expected to be installed
 * into the global default look up: {@code org.openide.util.Lookup.getDefault()}
 * via the {@code org.openide.util.lookup.ServiceProvider} annotation.
 * <P>
 * Instances of this interface must be safe to be called by multiple threads
 * concurrently but they are not required to be
 * <I>synchronization transparent</I> unless otherwise noted.
 *
 * @see org.netbeans.gradle.project.api.property.GradleProperty.BuildPlatform
 */
public interface GradleProjectPlatformQuery {
    /**
     * Registers a listener to be notified whenever the any of the platforms
     * managed by this query changes, a platform was removed or a new one is
     * available.
     * <P>
     * The listeners might be notified on any thread.
     *
     * @param listener the listener whose {@code run} method is to be called
     *   whenever a change occurs. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it may not be notified again. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public ListenerRef addPlatformChangeListener(@Nonnull Runnable listener);

    /**
     * Returns {@code true} if platforms with the specified name are to be
     * looked up by this query, {@code false} otherwise.
     * <P>
     * Returning {@code true} commits to that platforms of this name does not
     * need to be looked up elsewhere. This query should be able to find all
     * platforms with the given name.
     * <P>
     * Returning {@code false} means that platforms with the given name is
     * unlikely to be found by this query. So if this method returns {@code false},
     * the {@link #tryFindPlatformByName(String,String) tryFindPlatformByName}
     * will most likely return {@code null} if this name is specified. That is,
     * only look up platforms of unknown name as a last resort using this query.
     *
     * @param platformName the name of the queries platform. This argument
     *   cannot be {@code null}.
     * @return  {@code true} if platforms with the specified name are to be
     *   looked up by this query, {@code false} otherwise.
     *
     * @see ProjectPlatform#getName()
     * @see #tryFindPlatformByName(String, String)
     */
    public boolean isOwnerQuery(@Nonnull String platformName);

    /**
     * Returns the platforms accessible by this query. This method is used
     * to display the available options to the user to choose a platform.
     * <P>
     * It is not strictly required that this method returns all the platforms
     * available if finding all the platforms is practically infeasible (such as
     * looking up an entire hardrive) but platforms not returned by this method
     * cannot be conveniently chosen by the user.
     *
     * @return the platforms accessible by this query. This method never returns
     *   {@code null} and the returns collection may not contain {@code null}
     *   elements.
     */
    @Nonnull
    public Collection<ProjectPlatform> getAvailablePlatforms();

    /**
     * Finds and returns a platform best matching the given name and version or
     * {@code null} if there is no reasonable match.
     * <P>
     * This method should return a platform exactly matching the specified name
     * and version if available. However, if such platform is not available, it
     * might return one with a higher version number. As last resort it might
     * even return one with a lower version number in hope that the user only
     * uses features available in the chosen platform.
     *
     * @param name the name of the platform to be looked up. This argument
     *   cannot be {@code null}. In almost all cases the returned platform
     *   should have the same name as specified in this argument (though not
     *   strictly required).
     * @param version the version of the platform to be looked up. This argument
     *   cannot be {@code null}. The returned platform should have the same
     *   version as given in this argument if possible. This method however
     *   should not throw an exception regardless what the version string may
     *   contain.
     * @return a platform best matching the given name and version or
     *   {@code null} if there is no reasonable match
     *
     * @see ProjectPlatform#getName()
     * @see ProjectPlatform#getVersion()
     */
    @CheckForNull
    public ProjectPlatform tryFindPlatformByName(@Nonnull String name, @Nonnull String version);

    /**
     * Returns the project platform defined by the specified URI or {@code null}
     * if the URI does not define a valid platform.
     * <P>
     * This method is optional. That is, implementation may choose to return
     * {@code null} if they cannot find a platform by an URI or don't understand
     * the scheme of the URI.
     *
     * @param uri the {@code URI} defining the platform to be returned. This
     *   {@code URI} might define a directory of a local harddrive. This
     *   argument cannot be {@code null}.
     * @return the project platform defined by the specified URI or {@code null}
     *   if the URI does not define a valid platform
     */
    @CheckForNull
    public ProjectPlatform tryFindPlatformByUri(@Nonnull URI uri);
}
