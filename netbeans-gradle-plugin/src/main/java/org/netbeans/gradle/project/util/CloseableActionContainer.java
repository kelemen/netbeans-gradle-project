package org.netbeans.gradle.project.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.MonitorableTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.NbProperties;

public final class CloseableActionContainer {
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

    private void executeSync(CancelableTask task) {
        openSynchronizer.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
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
        SimpleListenerRegistry<Runnable> asRegistry = NbProperties.asChangeListenerRegistry(property);
        return defineEventAction(asRegistry, changeListener, executor);
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
        ExceptionHelper.checkNotNullArgument(listenerRegistry, "listenerRegistry");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        CloseableAction action = new CloseableAction() {
            @Override
            public CloseableAction.Ref open() {
                return toActionRef(listenerRegistry.registerListener(listener));
            }
        };
        return defineAction(PropertyFactory.constSource(action), executor);
    }

    private static CloseableAction.Ref toActionRef(final ListenerRef ref) {
        ExceptionHelper.checkNotNullArgument(ref, "ref");
        return new CloseableAction.Ref() {
            @Override
            public void close() {
                ref.unregister();
            }
        };
    }

    public CloseableAction.Ref defineAction(CloseableAction action) {
        return defineAction(action, SyncTaskExecutor.getSimpleExecutor());
    }

    public CloseableAction.Ref defineAction(CloseableAction action, TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(action, "action");
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

        executeSync(new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                if (opened) {
                    actionDef.open();
                }
            }
        });

        return new CloseableAction.Ref() {
            @Override
            public void close() {
                actionsLock.lock();
                try {
                    elementRef.remove();
                } finally {
                    actionsLock.unlock();
                }

                actionDef.closeForGood();
            }
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
        executeSync(new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                openNow();
            }
        });
    }

    public void close() {
        executeSync(new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                if (opened) {
                    closeNow();
                }
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
            ExceptionHelper.checkNotNullArgument(actionProperty, "actionProperty");
            ExceptionHelper.checkNotNullArgument(executor, "executor");

            this.actionProperty = actionProperty;
            this.actionChangeRef = null;
            this.syncExecutor = TaskExecutors.inOrderExecutor(executor);
            this.openedActionRef = null;
            this.closedForGood = false;
        }

        private void executeSync(CancelableTask task) {
            syncExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        }

        public void replaceAction() {
            executeSync(new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    closeNow();
                    openNow();
                }
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

            actionChangeRef = actionProperty.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    replaceAction();
                }
            });

            CloseableAction action = actionProperty.getValue();
            openedActionRef = action != null ? action.open() : null;
        }

        public void open() {
            executeSync(new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    if (openedActionRef == null) {
                        openNow();
                    }
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
            executeSync(new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    closeNow();
                }
            });
        }

        public void closeForGood() {
            executeSync(new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    closedForGood = true;
                    closeNow();
                }
            });
        }
    }
}
