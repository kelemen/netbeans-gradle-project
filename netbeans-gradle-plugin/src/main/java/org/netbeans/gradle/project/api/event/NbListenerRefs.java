package org.netbeans.gradle.project.api.event;

import javax.annotation.Nonnull;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;

/**
 * Contains utility methods related to {@link NbListenerRef}.
 */
public final class NbListenerRefs {
    /**
     * Converts {@code ListenerRef} instances to {@code NbListenerRef} instances.
     * All calls to the returned {@code NbListenerRef} are delegated to the
     * appropriate method of the specified {@code ListenerRef}.
     *
     * @param listenerRef the {@code ListenerRef} to be converted to
     *   {@code NbListenerRef}. This argument cannot be {@code null}.
     * @return the {@code NbListenerRef} delegating all of its calls to the
     *   {@code ListenerRef} specified in the argument. This method never
     *   returns {@code null}.
     */
    @Nonnull
    public static NbListenerRef asNbRef(@Nonnull ListenerRef listenerRef) {
        return listenerRef::unregister;
    }

    /**
     * Converts a {@code Runnable} to an {@link NbListenerRef}. The given task
     * is called in the {@code unregister} method of the returned
     * {@code NbListenerRef}. Also, it is ensured by the returned
     * {@code NbListenerRef} instance that the specified unregister task is not
     * called multiple times. That is, if {@code unregister} is called multiple
     * times, only the first {@code unregister} call will execute the specified
     * task, subsequent {@code unregister} calls will simply return immediately.
     *
     * @param unregisterTask the {@code Runnable} whose {@code run()} method is
     *   called in the {@code unregister} method of the returned
     *   {@code NbListenerRef}. This argument cannot be {@code null}.
     * @return the {@code NbListenerRef} relying on the given task in its
     *   {@code unregister} method. This method never returns {@code null}.
     */
    @Nonnull
    public static NbListenerRef fromRunnable(@Nonnull Runnable unregisterTask) {
        return Tasks.runOnceTask(unregisterTask)::run;
    }

    private NbListenerRefs() {
        throw new AssertionError();
    }
}
