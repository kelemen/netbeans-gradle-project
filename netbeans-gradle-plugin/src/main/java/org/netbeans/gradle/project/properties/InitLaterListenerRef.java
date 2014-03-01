package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.project.api.event.NbListenerRef;

/**
 * Defines a {@link NbListenerRef} forwarding its calls to another
 * {@code NbListenerRef} which is specified after construction time. This class
 * is useful if you need to unregister a listener in the listener itself (or in
 * any code which is defined before actually registering the listener).
 * <P>
 * This is the recommended usage pattern:
 * <pre>
 * {@literal ListenerRegistry<Runnable> registry = ...;}
 *
 * final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
 * // From this point, you may use "listenerRef" to unregister the listener.
 * // The listener will be unregistered as soon as the "init" method has been
 * // called
 *
 * listenerRef.init(registry.registerListener(new Runnable(){
 *   // You may use "listenerRef" here to unregister this listener.
 * }));
 * </pre>
 * <P>
 * Note that the actual unregistering of the listener will not happen until you
 * call the {@link #init(NbListenerRef)} method. Therefore, if you unregister
 * the listener prior calling {@code init}, it may be possible, that the listener
 * will be notified even if you have called {@code unregister()} on the
 * {@code InitLaterListenerRef} which is a violation of the contract of the
 * {@code NbListenerRef} interface. Therefore, it is not recommended not
 * unregister the listener before calling the {@code init} method. If you do
 * unregister it, don't forget that it is possible that you may receive event
 * notifications.
 * <P>
 * This class was copied from the <I>JTrim</I> library and will be removed once
 * the plugin may switch to use Java 7.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>, assuming
 * that the underlying listener is <I>synchronization transparent</I>.
 */
public final class InitLaterListenerRef implements NbListenerRef {
    private final AtomicReference<NbListenerRef> currentRef;

    /**
     * Creates a new {@code InitLaterListenerRef} with no underlying
     * {@code NbListenerRef}. Call the {@link #init(NbListenerRef) init} method,
     * to set the {@code NbListenerRef} to which calls are to be forwarded.
     */
    public InitLaterListenerRef() {
        this.currentRef = new AtomicReference<>(null);
    }

    private void completeUnregistration(NbListenerRef listenerRef) {
        assert listenerRef != null;
        try {
            listenerRef.unregister();
        } finally {
            currentRef.set(PoisonListenerRef.UNREGISTERED);
        }
    }

    /**
     * Sets to {@code NbListenerRef} to which calls are forwarded to. This method
     * may not be called more than once.
     * <P>
     * If {@link #unregister()} has been called on this
     * {@code InitLaterListenerRef}, this method will call the
     * {@code unregister()} method of the passed {@code NbListenerRef}.
     * <P>
     * Note that before calling this method, {@link #isRegistered()} will return
     * {@code true}.
     *
     * @param listenerRef the {@code NbListenerRef} to which class are forwarded
     *   to. This argument cannot be {@code null}.
     *
     * @throws IllegalStateException thrown if this method has already been
     *   called
     */
    public void init(NbListenerRef listenerRef) {
        if (listenerRef == null) throw new NullPointerException("listenerRef");

        do {
            NbListenerRef oldRef = currentRef.get();
            if (oldRef != null) {
                if (oldRef instanceof PoisonListenerRef) {
                    completeUnregistration(listenerRef);
                    return;
                }

                throw new IllegalStateException("Already initialized.");
            }
        } while (!currentRef.compareAndSet(null, listenerRef));
    }

    /**
     * {@inheritDoc }.
     * <P>
     * Implementation note: This method will always return {@code true} prior
     * calling the {@link #init(NbListenerRef) init} method.
     */
    @Override
    public boolean isRegistered() {
        NbListenerRef listenerRef = currentRef.get();
        return listenerRef != null ? listenerRef.isRegistered() : true;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: If you call this method before calling the
     * {@link #init(NbListenerRef) init} method, this method will simply cause
     * the subsequent {@code init} method to unregister the underlying
     * {@code NbListenerRef}.
     * <P>
     * This method is allowed to be called concurrently with the {@code init}
     * method.
     */
    @Override
    public void unregister() {
        do {
            NbListenerRef oldRef = currentRef.get();
            if (oldRef != null) {
                completeUnregistration(oldRef);
                return;
            }
        } while (!currentRef.compareAndSet(null, PoisonListenerRef.REGISTERED));
    }

    private enum PoisonListenerRef implements NbListenerRef {
        REGISTERED(true), // used before true unregistration
        UNREGISTERED(false); // used after true unregistration

        private final boolean registered;

        private PoisonListenerRef(boolean registered) {
            this.registered = registered;
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        @Override
        public void unregister() {
        }
    }
}
