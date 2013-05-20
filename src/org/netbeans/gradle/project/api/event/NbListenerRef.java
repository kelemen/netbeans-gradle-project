package org.netbeans.gradle.project.api.event;

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
 */
public interface NbListenerRef {
    /**
     * Checks whether the listener is currently registered to receive
     * notifications of events.
     * <P>
     * In case this method returns {@code true}, the method
     * {@link #unregister() unregister()} can be called to stop the listener
     * from receiving notifications of the events occurring. If this method
     * returns {@code false}, the listener will not be notified of subsequent
     * events occurring. Also after this method returns {@code false}, the
     * {@code unregister()} method does nothing.
     *
     * @return {@code true} if the listener is currently registered to be
     *   notified of occurring events, {@code false} if the listener will not
     *   be notified of subsequent events
     */
    public boolean isRegistered();

    /**
     * Unregisters the listener, so it will not be notified of subsequent
     * events. That is, in case this method invocation <I>happen-before</I> the
     * occurring of an event: The listener will not be notified of that event.
     * <P>
     * This method must be idempotent. That is, invoking it multiple times has
     * the same effect as invoking it only once.
     */
    public void unregister();
}