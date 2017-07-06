package org.netbeans.gradle.project.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.swing.concurrent.SwingExecutors;

public final class GenericChangeListenerManager implements PausableChangeListenerManager {
    private final ChangeListenerManager wrapped;
    private final AtomicInteger pauseCount;
    private final AtomicBoolean hasUnfired;
    private final Runnable dispatcher;

    public GenericChangeListenerManager() {
        this(newDefaultListenerManager());
    }

    public GenericChangeListenerManager(TaskExecutor eventExecutor) {
        this(newDefaultListenerManager(), eventExecutor);
    }

    public GenericChangeListenerManager(ChangeListenerManager wrapped) {
        this(wrapped, (UpdateTaskExecutor)null);
    }

    public GenericChangeListenerManager(ChangeListenerManager wrapped, TaskExecutor eventExecutor) {
        this(wrapped, new GenericUpdateTaskExecutor(eventExecutor));
    }

    private GenericChangeListenerManager(ChangeListenerManager wrapped, UpdateTaskExecutor executor) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
        this.pauseCount = new AtomicInteger(0);
        this.hasUnfired = new AtomicBoolean(false);

        Runnable forwarder = this::fireEventNow;
        this.dispatcher = executor != null
                ? () -> executor.execute(forwarder)
                : forwarder;
    }

    private static ChangeListenerManager newDefaultListenerManager() {
        final ListenerManager<Runnable> listeners = new CopyOnTriggerListenerManager<>();
        return new ChangeListenerManager() {
            @Override
            public void fireEventually() {
                EventListeners.dispatchRunnable(listeners);
            }

            @Override
            public ListenerRef registerListener(Runnable listener) {
                return listeners.registerListener(listener);
            }

            @Override
            public int getListenerCount() {
                return listeners.getListenerCount();
            }
        };
    }

    public static GenericChangeListenerManager getSwingNotifier() {
        return new GenericChangeListenerManager(SwingExecutors.getStrictExecutor(false));
    }

    @Override
    public PauseRef pauseManager() {
        final Runnable unpauseTask = Tasks.runOnceTask(() -> {
            if (pauseCount.decrementAndGet() == 0) {
                if (hasUnfired.getAndSet(false)) {
                    fireEventually();
                }
            }
        });

        pauseCount.incrementAndGet();
        return unpauseTask::run;
    }

    private void fireEventNow() {
        if (pauseCount.get() == 0) {
            wrapped.fireEventually();
        }
        else {
            hasUnfired.set(true);
        }
    }

    @Override
    public void fireEventually() {
        dispatcher.run();
    }

    @Override
    public ListenerRef registerListener(Runnable listener) {
        return wrapped.registerListener(listener);
    }

    @Override
    public int getListenerCount() {
        return wrapped.getListenerCount();
    }
}
