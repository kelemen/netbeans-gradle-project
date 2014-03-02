package org.netbeans.gradle.project.api.event;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains utility methods related to {@link NbListenerRef}.
 */
public final class NbListenerRefs {
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
    public static NbListenerRef fromRunnable(@Nonnull final Runnable unregisterTask) {
        ExceptionHelper.checkNotNullArgument(unregisterTask, "unregisterTask");

        return new NbListenerRef() {
            private volatile boolean registered = true;
            private final AtomicBoolean executedTask = new AtomicBoolean(false);

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                if (executedTask.compareAndSet(false, true)) {
                    unregisterTask.run();
                }
            }
        };
    }

    private NbListenerRefs() {
        throw new AssertionError();
    }
}
