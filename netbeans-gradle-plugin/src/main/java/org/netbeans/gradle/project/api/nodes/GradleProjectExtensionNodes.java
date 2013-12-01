package org.netbeans.gradle.project.api.nodes;

import java.util.List;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.event.NbListenerRef;

/**
 * Defines a query which returns the nodes to be added under the project node.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup()
 *
 * @author Kelemen Attila
 */
public interface GradleProjectExtensionNodes {
    /**
     * Registers a listener to be notified when the result of the
     * {@link #getNodeFactories()} method changes.
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
    public NbListenerRef addNodeChangeListener(@Nonnull Runnable listener);

    /**
     * Returns the factory for the list of nodes to be displayed below the
     * project node.
     *
     * @return the factory for the list of nodes to be displayed below the
     *   project node in the order they are to be displayed. This method never
     *   returns {@code null} but may return an empty list.
     */
    @Nonnull
    public List<SingleNodeFactory> getNodeFactories();
}
