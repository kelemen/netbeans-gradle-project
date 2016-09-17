package org.netbeans.gradle.project.util;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.DelegatedTaskExecutorService;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.SingleThreadedExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public final class NbTaskExecutors {
    private static final Logger LOGGER = Logger.getLogger(NbTaskExecutors.class.getName());

    public static final MonitorableTaskExecutorService DEFAULT_EXECUTOR
            = newExecutor("Gradle-Default", getDefaultThreadCount(), 5000);

    private static final long DEFAULT_IDLE_TIMEOUT_MS = 1000;

    public static MonitorableTaskExecutorService newExecutor(String name, int threadCount) {
        return newExecutor(name, threadCount, DEFAULT_IDLE_TIMEOUT_MS);
    }

    public static MonitorableTaskExecutorService newExecutor(String name, int threadCount, long timeoutMs) {
        return new Unstoppable(newStoppableExecutor(name, threadCount));
    }

    public static MonitorableTaskExecutorService newStoppableExecutor(String name, int threadCount) {
        return newStoppableExecutor(name, threadCount, DEFAULT_IDLE_TIMEOUT_MS);
    }

    public static MonitorableTaskExecutorService newStoppableExecutor(String name, int threadCount, long timeoutMs) {
        ExceptionHelper.checkArgumentInRange(threadCount, 1, Integer.MAX_VALUE, "threadCount");

        if (threadCount == 1) {
            return new SingleThreadedExecutor(name, Integer.MAX_VALUE, timeoutMs, TimeUnit.MILLISECONDS);
        }
        else {
            return new ThreadPoolTaskExecutor(name, threadCount, Integer.MAX_VALUE, timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    public static MonitorableTaskExecutor newDefaultFifoExecutor() {
        return TaskExecutors.inOrderExecutor(NbTaskExecutors.DEFAULT_EXECUTOR);
    }

    public static UpdateTaskExecutor newDefaultUpdateExecutor() {
        return new GenericUpdateTaskExecutor(TaskExecutors.inOrderSimpleExecutor(NbTaskExecutors.DEFAULT_EXECUTOR));
    }

    private static int getDefaultThreadCount() {
        // We don't want too much thread, because there is little benefit
        // and many threads might need much more memory.
        return Math.min(Runtime.getRuntime().availableProcessors(), 8);
    }

    public static void defaultCleanup(boolean canceled, Throwable error) {
        if (error == null || (canceled && error instanceof OperationCanceledException)) {
            return;
        }

        LOGGER.log(Level.SEVERE, "Uncaught exception in task.", error);
    }

    private static final class Unstoppable
    extends
            DelegatedTaskExecutorService
    implements
            MonitorableTaskExecutorService {

        private final MonitorableTaskExecutorService wrappedMonitorable;

        public Unstoppable(MonitorableTaskExecutorService wrappedExecutor) {
            super(wrappedExecutor);

            this.wrappedMonitorable = wrappedExecutor;
        }

        @Override
        public long getNumberOfQueuedTasks() {
            return wrappedMonitorable.getNumberOfQueuedTasks();
        }

        @Override
        public long getNumberOfExecutingTasks() {
            return wrappedMonitorable.getNumberOfExecutingTasks();
        }

        @Override
        public boolean isExecutingInThis() {
            return wrappedMonitorable.isExecutingInThis();
        }

        @Override
        public void shutdownAndCancel() {
            shutdown();
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException("This executor cannot be shutted down.");
        }
    }

    private NbTaskExecutors() {
        throw new AssertionError();
    }
}
