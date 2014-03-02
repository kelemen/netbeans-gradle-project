package org.netbeans.gradle.project.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.openide.util.Cancellable;

public final class GradleDaemonManager {
    private static final Logger LOGGER = Logger.getLogger(GradleDaemonManager.class.getName());

    private static final ReentrantLock QUEUE_LOCK = new ReentrantLock(true);

    private static void runNonBlockingGradleTask(DaemonTask task, ProgressHandle progress) throws InterruptedException {
        progress.suspend("");
        QUEUE_LOCK.lockInterruptibly();
        try {
            progress.switchToIndeterminate();
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

        progress.switchToIndeterminate();
        task.run(progress);
    }

    public static boolean isRunningExclusiveTask() {
        return QUEUE_LOCK.isHeldByCurrentThread();
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            String caption,
            DaemonTask task,
            boolean nonBlocking,
            CommandCompleteListener listener) {
        submitGradleTask(executor, new DaemonTaskDef(caption, nonBlocking, task), listener);
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            final DaemonTaskDef taskDef,
            CommandCompleteListener listener) {
        submitGradleTask(executor, taskDef.toFactory(), listener);
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            final DaemonTaskDefFactory taskDefFactory,
            final CommandCompleteListener listener) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(taskDefFactory, "taskDefFactory");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                DaemonTaskDef taskDef;
                try {
                    taskDef = taskDefFactory.tryCreateTaskDef();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to create DaemonTaskDef.", ex);
                    return;
                }

                if (taskDef == null) {
                    return;
                }

                String displayName = taskDef.getCaption();
                boolean nonBlocking = taskDef.isNonBlocking();
                DaemonTask task = taskDef.getTask();

                final ThreadInterrupter interrupter = new ThreadInterrupter(Thread.currentThread());

                // TODO: Create the ProgressHandle before starting the task
                //       This requires a DaemonTaskDefFactory returning the caption.
                final CancellationSource cancel = Cancellation.createChildCancellationSource(cancelToken);
                ProgressHandle progress = ProgressHandleFactory.createHandle(displayName, new Cancellable() {
                    @Override
                    public boolean cancel() {
                        cancel.getController().cancel();
                        return true;
                    }
                });

                // TODO: Forward the cancellation token.
                progress.start();
                try {
                    if (nonBlocking) {
                        runNonBlockingGradleTask(task, progress);
                    }
                    else {
                        runBlockingGradleTask(task, progress);
                    }
                } finally {
                    interrupter.stopInterrupting();
                    progress.finish();
                }
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                listener.onComplete(canceled ? null : error);
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
