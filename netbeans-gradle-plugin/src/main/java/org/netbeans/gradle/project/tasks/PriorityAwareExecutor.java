package org.netbeans.gradle.project.tasks;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.InitLaterListenerRef;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.utils.ExceptionHelper;

public final class PriorityAwareExecutor {
    private final TaskExecutor wrapped;
    private final TaskQueue taskQueue;

    public PriorityAwareExecutor(TaskExecutor wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

        this.wrapped = wrapped;
        this.taskQueue = new TaskQueue();
    }

    private void executeForPriority(
            CancellationToken cancelToken,
            Priority priority,
            CancelableTask task,
            CleanupTask cleanupTask) {

        TaskDef taskDef = new TaskDef(cancelToken, task, cleanupTask);
        RefCollection.ElementRef<?> queueRef = taskQueue.addTask(priority, taskDef);
        taskDef.init(queueRef);

        AtomicReference<TaskDef> taskDefRef = new AtomicReference<>(null);

        wrapped.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken taskCancelToken) -> {
            TaskDef nextTask = taskQueue.pollTask();
            taskDefRef.set(nextTask);
            nextTask.doTask(taskCancelToken);
        }, (boolean canceled, Throwable error) -> {
            TaskDef def = taskDefRef.get();
            if (def != null) {
                def.cleanup(canceled, error);
            }
            else if (canceled) {
                // This means, that the executor has been terminated
                // so poll one cleanup task and execute it.
                TaskDef task1 = taskQueue.pollTask();
                if (task1 != null) {
                    task1.cleanup(canceled, error);
                }
            }
        });
    }

    private TaskExecutor getExecutor(final Priority priority) {
        return (CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) -> {
            executeForPriority(cancelToken, priority, task, cleanupTask);
        };
    }

    public TaskExecutor getHighPriorityExecutor() {
        return getExecutor(Priority.HIGH);
    }

    public TaskExecutor getLowPriorityExecutor() {
        return getExecutor(Priority.LOW);
    }

    private static final class TaskQueue {
        private final Lock queueLock;
        // TODO: Allow arbitrary priority
        private final RefLinkedList<TaskDef> queueHighPriority;
        private final RefLinkedList<TaskDef> queueLowPriority;

        public TaskQueue() {
            this.queueLock = new ReentrantLock();
            this.queueLowPriority = new RefLinkedList<>();
            this.queueHighPriority = new RefLinkedList<>();
        }

        private RefLinkedList<TaskDef> getQueue(Priority priority) {
            return priority == Priority.HIGH ? queueHighPriority : queueLowPriority;
        }

        private static <E> RefCollection.ElementRef<E> wrapLocked(final RefCollection.ElementRef<E> ref, final Lock lock) {
            return new RefCollection.ElementRef<E>() {
                @Override
                public E setElement(E newElement) {
                    throw new UnsupportedOperationException("Cannot update element");
                }

                @Override
                public E getElement() {
                    return ref.getElement();
                }

                @Override
                public boolean isRemoved() {
                    lock.lock();
                    try {
                        return ref.isRemoved();
                    } finally {
                        lock.unlock();
                    }
                }

                @Override
                public void remove() {
                    lock.lock();
                    try {
                        ref.remove();
                    } finally {
                        lock.unlock();
                    }
                }
            };
        }

        public RefCollection.ElementRef<?> addTask(Priority priority, TaskDef task) {
            RefLinkedList<TaskDef> queue = getQueue(priority);

            queueLock.lock();
            try {
                return wrapLocked(queue.addLastGetReference(task), queueLock);
            } finally {
                queueLock.unlock();
            }
        }

        public TaskDef pollTask() {
            queueLock.lock();
            try {
                TaskDef result = queueHighPriority.poll();
                if (result == null) {
                    result = queueLowPriority.poll();
                }
                return result;
            } finally {
                queueLock.unlock();
            }
        }
    }

    private enum Priority {
        HIGH,
        LOW
    }

    private static final class TaskDef {
        private volatile CancellationToken cancelToken;
        private volatile CancelableTask task;
        private volatile boolean skippedExecute;
        private final CleanupTask cleanupTask;

        private final AtomicReference<ListenerRef> cancelRef;

        public TaskDef(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            this.cancelToken = cancelToken;
            this.task = task;
            this.cleanupTask = cleanupTask;
            this.cancelRef = new AtomicReference<>(null);
            this.skippedExecute = false;
        }

        public void init(final RefCollection.ElementRef<?> queueRef) {
            final InitLaterListenerRef cancelRefRef = new InitLaterListenerRef();

            cancelRefRef.init(cancelToken.addCancellationListener(() -> {
                removeTask();

                if (cleanupTask == null) {
                    queueRef.remove();
                }

                cancelRefRef.unregister();
            }));

            if (!this.cancelRef.compareAndSet(null, cancelRefRef)) {
                cancelRefRef.unregister();
            }
        }

        public void removeTask() {
            task = null;
            cancelToken = null;
        }

        public void doTask(CancellationToken executorCancelToken) throws Exception {
            CancellationToken currentCancelToken = cancelToken;
            currentCancelToken = currentCancelToken != null
                    ? Cancellation.anyToken(executorCancelToken, currentCancelToken)
                    : executorCancelToken;

            CancelableTask currentTask = task;
            if (currentTask != null) {
                currentTask.execute(currentCancelToken);
            }
            else {
                skippedExecute = true;
            }
        }

        public void cleanup(boolean canceled, Throwable error) throws Exception {
            try {
                ListenerRef currentCancelRef = cancelRef.getAndSet(UnregisteredListenerRef.INSTANCE);
                if (currentCancelRef != null) {
                    currentCancelRef.unregister();
                }
            } finally {
                if (cleanupTask != null) {
                    cleanupTask.cleanup(canceled || skippedExecute, error);
                }
            }
        }
    }
}
