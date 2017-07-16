package org.netbeans.gradle.project.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.collections.RefCollection;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.SimpleListenerRegistry;
import org.jtrim2.executor.MonitorableTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;

public final class CloseableActionContainer {
    private static final Logger LOGGER = Logger.getLogger(CloseableActionContainer.class.getName());

    private final Lock actionsLock;
    private final RefList<ActionDef> actions;

    private final MonitorableTaskExecutor openSynchronizer;
    private boolean opened;

    public CloseableActionContainer() {
        this.actionsLock = new ReentrantLock();
        this.actions = new RefLinkedList<>();
        this.openSynchronizer = TaskExecutors.inOrderSyncExecutor();
        this.opened = false;
    }

    public static CloseableAction mergeActions(CloseableAction... actions) {
        final CloseableAction[] actionsCopy = actions.clone();
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        return () -> {
            CloseableAction.Ref[] result = new CloseableAction.Ref[actionsCopy.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = actionsCopy[i].open();
            }
            return mergeActionRefs(result);
        };
    }

    private static CloseableAction.Ref mergeActionRefs(final CloseableAction.Ref[] refs) {
        return () -> {
            for (CloseableAction.Ref ref: refs) {
                try {
                    ref.close();
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected close exception.", ex);
                }
            }
        };
    }

    private void executeSync(Runnable task) {
        openSynchronizer.execute(task);
    }

    public <Listener> CloseableAction.Ref definePropertyChangeAction(
            final PropertySource<?> property,
            final Runnable changeListener) {
        return definePropertyChangeAction(property, changeListener, SyncTaskExecutor.getSimpleExecutor());
    }

    public <Listener> CloseableAction.Ref definePropertyChangeAction(
            final PropertySource<?> property,
            final Runnable changeListener,
            TaskExecutor executor) {
        return defineEventAction(property::addChangeListener, changeListener, executor);
    }

    public <Listener> CloseableAction.Ref defineEventAction(
            final SimpleListenerRegistry<Listener> listenerRegistry,
            final Listener listener) {
        return defineEventAction(listenerRegistry, listener, SyncTaskExecutor.getSimpleExecutor());
    }

    public <Listener> CloseableAction.Ref defineEventAction(
            final SimpleListenerRegistry<Listener> listenerRegistry,
            final Listener listener,
            TaskExecutor executor) {
        Objects.requireNonNull(listenerRegistry, "listenerRegistry");
        Objects.requireNonNull(listener, "listener");

        CloseableAction action = () -> toActionRef(listenerRegistry.registerListener(listener));
        return defineAction(PropertyFactory.constSource(action), executor);
    }

    private static CloseableAction.Ref toActionRef(ListenerRef ref) {
        return ref::unregister;
    }

    public CloseableAction.Ref defineAction(CloseableAction action) {
        return defineAction(action, SyncTaskExecutor.getSimpleExecutor());
    }

    public CloseableAction.Ref defineAction(CloseableAction action, TaskExecutor executor) {
        Objects.requireNonNull(action, "action");
        return defineAction(PropertyFactory.constSource(action), executor);
    }

    public CloseableAction.Ref defineAction(PropertySource<? extends CloseableAction> actionProperty) {
        return defineAction(actionProperty, SyncTaskExecutor.getSimpleExecutor());
    }

    public CloseableAction.Ref defineAction(PropertySource<? extends CloseableAction> actionProperty, TaskExecutor executor) {
        final ActionDef actionDef = new ActionDef(actionProperty, executor);

        final RefCollection.ElementRef<ActionDef> elementRef;
        actionsLock.lock();
        try {
            elementRef = actions.addGetReference(actionDef);
        } finally {
            actionsLock.unlock();
        }

        executeSync(() -> {
            if (opened) {
                actionDef.open();
            }
        });

        return () -> {
            actionsLock.lock();
            try {
                elementRef.remove();
            } finally {
                actionsLock.unlock();
            }

            actionDef.closeForGood();
        };
    }

    private List<ActionDef> getActionDefs() {
        actionsLock.lock();
        try {
            return new ArrayList<>(actions);
        } finally {
            actionsLock.unlock();
        }
    }

    private void openNow() {
        assert openSynchronizer.isExecutingInThis();
        if (opened) {
            return;
        }
        opened = true;

        for (ActionDef action: getActionDefs()) {
            action.open();
        }
    }

    private void closeNow() {
        assert openSynchronizer.isExecutingInThis();
        if (!opened) {
            return;
        }
        opened = false;

        for (ActionDef action: getActionDefs()) {
            action.close();
        }
    }

    public void open() {
        executeSync(this::openNow);
    }

    public void close() {
        executeSync(() -> {
            if (opened) {
                closeNow();
            }
        });
    }

    public static interface AddedActionRef extends AutoCloseable {
        public void replaceAction(CloseableAction action);

        @Override
        public void close();
    }

    private static final class ActionDef {
        private final MonitorableTaskExecutor syncExecutor;

        private final PropertySource<? extends CloseableAction> actionProperty;
        private CloseableAction.Ref openedActionRef;
        private ListenerRef actionChangeRef;

        private boolean closedForGood;

        public ActionDef(PropertySource<? extends CloseableAction> actionProperty, TaskExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            this.actionProperty = Objects.requireNonNull(actionProperty, "actionProperty");
            this.actionChangeRef = null;
            this.syncExecutor = TaskExecutors.inOrderExecutor(executor);
            this.openedActionRef = null;
            this.closedForGood = false;
        }

        private void executeSync(Runnable task) {
            syncExecutor.execute(task);
        }

        public void replaceAction() {
            executeSync(() -> {
                closeNow();
                openNow();
            });
        }

        private void openNow() {
            assert openedActionRef == null;

            if (closedForGood) {
                return;
            }

            ListenerRef prevActionChangeRef = actionChangeRef;
            if (prevActionChangeRef != null) {
                prevActionChangeRef.unregister();
            }

            actionChangeRef = actionProperty.addChangeListener(this::replaceAction);

            CloseableAction action = actionProperty.getValue();
            openedActionRef = action != null ? action.open() : null;
        }

        public void open() {
            executeSync(() -> {
                if (openedActionRef == null) {
                    openNow();
                }
            });
        }

        private void closeNow() {
            assert syncExecutor.isExecutingInThis();

            ListenerRef prevActionChangeRef = actionChangeRef;
            if (prevActionChangeRef != null) {
                actionChangeRef = null;
                prevActionChangeRef.unregister();
            }

            CloseableAction.Ref toClose = openedActionRef;
            if (toClose != null) {
                openedActionRef = null;
                toClose.close();
            }
        }

        public void close() {
            executeSync(this::closeNow);
        }

        public void closeForGood() {
            executeSync(() -> {
                closedForGood = true;
                closeNow();
            });
        }
    }
}
