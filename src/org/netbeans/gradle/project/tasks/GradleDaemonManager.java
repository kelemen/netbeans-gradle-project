package org.netbeans.gradle.project.tasks;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;

public final class GradleDaemonManager {
    private static final Logger LOGGER = Logger.getLogger(GradleDaemonManager.class.getName());

    private static final Lock QUEUE_LOCK = new ReentrantLock(true);

    private static void runNonBlockingGradleTask(DaemonTask task, ProgressHandle progress) throws InterruptedException {
        progress.suspend("");
        QUEUE_LOCK.lockInterruptibly();
        try {
            progress.progress("");
            task.run(progress);
        } finally{
            QUEUE_LOCK.unlock();
        }
    }

    private static void runBlockingGradleTask(DaemonTask task, ProgressHandle progress) throws InterruptedException {
        progress.suspend("");

        // This lock/unlock is here only to wait for pending non-blocking tasks.
        QUEUE_LOCK.lockInterruptibly();
        QUEUE_LOCK.unlock();

        progress.progress("");
        task.run(progress);
    }

    public static void submitGradleTask(
            Executor executor,
            final String displayName,
            final DaemonTask task,
            final boolean nonBlocking) {
        if (executor == null) throw new NullPointerException("executor");
        if (displayName == null) throw new NullPointerException("displayName");
        if (task == null) throw new NullPointerException("task");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final ThreadInterrupter interrupter = new ThreadInterrupter(Thread.currentThread());
                ProgressHandle progress = ProgressHandleFactory.createHandle(displayName, new Cancellable() {
                    @Override
                    public boolean cancel() {
                        interrupter.interrupt();
                        return true;
                    }
                });

                progress.start();
                try {
                    if (nonBlocking) {
                        runNonBlockingGradleTask(task, progress);
                    }
                    else {
                        runBlockingGradleTask(task, progress);
                    }
                } catch (InterruptedException ex) {
                    // We must hide InterruptedException because we use it
                    // for our own purpose: To signal that a task must be
                    // canceled.
                    // In case executors don't use interrupt for other purposes
                    // (and they should not), this shouldn't cause problems.
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Unexpected exception in Gradle daemon task.", ex);
                } finally {
                    interrupter.stopInterrupting();
                    progress.finish();
                }
            }
        });
    }

    private static final class ThreadInterrupter {
        private final Lock mainLock;
        private Thread thread;

        public ThreadInterrupter(Thread thread) {
            this.mainLock = new ReentrantLock();
            this.thread = thread;
        }

        public void interrupt() {
            mainLock.lock();
            try {
                if (thread != null) {
                    thread.interrupt();
                }
            } finally {
                mainLock.unlock();
            }
        }

        public void stopInterrupting() {
            mainLock.lock();
            try {
                thread = null;
            } finally {
                mainLock.unlock();
            }
        }
    }

    private GradleDaemonManager() {
        throw new AssertionError();
    }
}
