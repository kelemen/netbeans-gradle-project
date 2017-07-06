package org.netbeans.gradle.project.event;

import java.util.Objects;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.OneShotListenerManager;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.swing.concurrent.SwingExecutors;

public final class OneShotChangeListenerManager implements PausableChangeListenerManager {
    private final OneShotListenerManager<Runnable, Void> oneShotManager;
    private final PausableChangeListenerManager pausableManager;

    public OneShotChangeListenerManager() {
        this(new OneShotListenerManager<Runnable, Void>(), null);
    }

    public OneShotChangeListenerManager(TaskExecutor executor) {
        this(new OneShotListenerManager<Runnable, Void>(), Objects.requireNonNull(executor, "executor"));
    }

    private OneShotChangeListenerManager(
            OneShotListenerManager<Runnable, Void> oneShotManager,
            TaskExecutor executor) {
        this.oneShotManager = oneShotManager;
        this.pausableManager = executor != null
                ? new GenericChangeListenerManager(wrapOneShotManager(oneShotManager), executor)
                : new GenericChangeListenerManager(wrapOneShotManager(oneShotManager));
    }

    private static ChangeListenerManager wrapOneShotManager(final OneShotListenerManager<Runnable, Void> wrapped) {
        assert wrapped != null;

        return new ChangeListenerManager() {
            @Override
            public void fireEventually() {
                EventListeners.dispatchRunnable(wrapped);
            }

            @Override
            public ListenerRef registerListener(Runnable listener) {
                return wrapped.registerListener(listener);
            }

            @Override
            public int getListenerCount() {
                return wrapped.getListenerCount();
            }
        };
    }

    public static OneShotChangeListenerManager getSwingNotifier() {
        return new OneShotChangeListenerManager(SwingExecutors.getStrictExecutor(false));
    }

    public ListenerRef registerOrNotifyListener(Runnable listener) {
        return oneShotManager.registerOrNotifyListener(listener);
    }

    @Override
    public PauseRef pauseManager() {
        return pausableManager.pauseManager();
    }

    @Override
    public void fireEventually() {
        pausableManager.fireEventually();
    }

    @Override
    public ListenerRef registerListener(Runnable listener) {
        return pausableManager.registerListener(listener);
    }

    @Override
    public int getListenerCount() {
        return pausableManager.getListenerCount();
    }
}
