package org.netbeans.gradle.project.api.event;

import org.jtrim.event.ListenerRef;

/**
 * Defines a reference of an event handler which has been registered to be
 * notified of a certain event. The event handler can be
 * {@link #unregister() unregistered} once no longer needed to be notified of
 * the event. Once a listener has been unregistered, there is no way to register
 * it again through this interface. That is, it should be registered as it was
 * done previously.
 * <P>
 * Note that this interface defines the same methods as the
 * {@code org.jtrim.event.ListenerRef} (of the <I>JTrim</I> library) interface
 * with exactly the same contract. This is so, that later (in NetBeans 7.4) this
 * interface can extend from the interface of <I>JTrim</I>.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I>.
 *
 * @see NbListenerRefs
 * @see ListenerRef
 */
public interface NbListenerRef extends ListenerRef {
}