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
 * This interface exists for backward compatibilty reasons only, you
 * should use {@code org.jtrim.event.ListenerRef} whenever possible. If
 * you need to convert a {@code ListenerRef} to this interface, use the
 * {@link NbListenerRefs#asNbRef(org.jtrim.event.ListenerRef) NbListenerRefs.asNbRef}
 * method.
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