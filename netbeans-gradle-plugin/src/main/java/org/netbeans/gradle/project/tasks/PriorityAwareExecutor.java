package org.netbeans.gradle.project.tasks;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.RefCollection;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.event.InitLaterListenerRef;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.TaskExecutor;

public final class PriorityAwareExecutor {
    private final TaskExecutor wrapped;
    private final TaskQueue taskQueue;

    public PriorityAwareExecutor(TaskExecutor wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
        this.taskQueue = new TaskQueue();
    }

    private <V> CompletionStage<V> executeForPriority(
            CancellationToken cancelToken,
            Priority priority,
            CancelableFunction<? extends V> task) {

        TaskDef<V> taskDef = new TaskDef<>(cancelToken, task);
        RefCollection.ElementRef<?> queueRef = taskQueue.addTask(priority, taskDef);
        taskDef.init(queueRef);

        wrapped.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken taskCancelToken) -> {
            TaskDef<?> nextTask = taskQueue.pollTask();
            nextTask.doTask(taskCancelToken);
        });

        return taskDef.future;
    }

    private TaskExecutor getExecutor(Priority priority) {
        return new PriorityExecutorImpl(priority);
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
        private final RefLinkedList<TaskDef<?>> queueHighPriority;
        private final RefLinkedList<TaskDef<?>> queueLowPriority;

        public TaskQueue() {
            this.queueLock = new ReentrantLock();
            this.queueLowPriority = new RefLinkedList<>();
            this.queueHighPriority = new RefLinkedList<>();
        }

        private RefLinkedList<TaskDef<?>> getQueue(Priority priority) {
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

        public RefCollection.ElementRef<?> addTask(Priority priority, TaskDef<?> task) {
            RefLinkedList<TaskDef<?>> queue = getQueue(priority);

            queueLock.lock();
            try {
                return wrapLocked(queue.addLastGetReference(task), queueLock);
            } finally {
                queueLock.unlock();
            }
        }

        public TaskDef<?> pollTask() {
            queueLock.lock();
            try {
                TaskDef<?> result = queueHighPriority.poll();
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

    private static final class TaskDef<V> {
        private volatile CancellationToken cancelToken;
        private volatile CancelableFunction<? extends V> task;
        private final CompletableFuture<V> future;

        private final AtomicReference<ListenerRef> cancelRef;

        public TaskDef(CancellationToken cancelToken, CancelableFunction<? extends V> task) {
            this.cancelToken = cancelToken;
            this.task = task;
            this.future = new CompletableFuture<>();
            this.cancelRef = new AtomicReference<>(null);
        }

        public void init(final RefCollection.ElementRef<?> queueRef) {
            final InitLaterListenerRef cancelRefRef = new InitLaterListenerRef();

            cancelRefRef.init(cancelToken.addCancellationListener(() -> {
                removeTask();

                future.completeExceptionally(OperationCanceledException.withoutStackTrace());
                queueRef.remove();

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
            try {
                CancellationToken currentCancelToken = cancelToken;
                currentCancelToken = currentCancelToken != null
                        ? Cancellation.anyToken(executorCancelToken, currentCancelToken)
                        : executorCancelToken;

                CancelableFunction<? extends V> currentTask = task;
                if (currentTask == null) {
                    future.completeExceptionally(OperationCanceledException.withoutStackTrace());
                    return;
                }

                V result = currentTask.execute(currentCancelToken);
                future.complete(result);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        }
    }

    private final class PriorityExecutorImpl implements TaskExecutor {
        private final Priority priority;

        public PriorityExecutorImpl(Priority priority) {
            this.priority = priority;
        }

        @Override
        public <V> CompletionStage<V> executeFunction(CancellationToken cancelToken, CancelableFunction<? extends V> function) {
            return executeForPriority(cancelToken, priority, function);
        }
    }
}
