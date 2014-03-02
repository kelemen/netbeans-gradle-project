package org.netbeans.gradle.project;

import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.DelegatedTaskExecutorService;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.SingleThreadedExecutor;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public final class NbTaskExecutors {
    private static final long IDLE_TIMEOUT_MS = 1000;

    public static MonitorableTaskExecutorService newExecutor(String name, int threadCount) {
        return new Unstoppable(newStoppableExecutor(name, threadCount));
    }

    public static MonitorableTaskExecutorService newStoppableExecutor(String name, int threadCount) {
        ExceptionHelper.checkArgumentInRange(threadCount, 1, Integer.MAX_VALUE, "threadCount");

        if (threadCount == 1) {
            return new SingleThreadedExecutor(name, Integer.MAX_VALUE, IDLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
        else {
            return new ThreadPoolTaskExecutor(name, threadCount, Integer.MAX_VALUE, IDLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
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
