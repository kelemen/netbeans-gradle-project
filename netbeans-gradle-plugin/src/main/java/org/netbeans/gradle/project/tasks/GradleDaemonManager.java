package org.netbeans.gradle.project.tasks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
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

    private static void runNonBlockingGradleTask(
            CancellationToken cancelToken,
            DaemonTask task,
            ProgressHandle progress) {

        progress.suspend("");

        CancelableWaits.lock(cancelToken, QUEUE_LOCK);
        try {
            progress.switchToIndeterminate();
            task.run(cancelToken, progress);
        } finally{
            QUEUE_LOCK.unlock();
        }
    }

    private static void runBlockingGradleTask(
            CancellationToken cancelToken,
            DaemonTask task,
            ProgressHandle progress) {

        progress.suspend("");

        // This lock/unlock is here only to wait for pending non-blocking tasks.
        CancelableWaits.lock(cancelToken, QUEUE_LOCK);
        QUEUE_LOCK.unlock();

        progress.switchToIndeterminate();
        task.run(cancelToken, progress);
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

        final CancellationSource cancel = Cancellation.createCancellationSource();
        final String origDisplayName = taskDefFactory.getDisplayName();

        final ReplaceableProgressHandle progress = new ReplaceableProgressHandle(cancel.getController());

        progress.start(origDisplayName);
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

                if (!Objects.equals(displayName, origDisplayName)) {
                    progress.start(displayName);
                }

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
                        runNonBlockingGradleTask(cancel.getToken(), task, progress);
                    }
                    else {
                        runBlockingGradleTask(cancel.getToken(), task, progress);
                    }
                } finally {
                    progress.finish();
                }
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                try {
                    listener.onComplete(canceled ? null : error);
                } finally {
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

    private static final class ReplaceableProgressHandle {
        private final AtomicReference<ProgressHandle> handleRef;
        private final CancellationController cancelController;

        public ReplaceableProgressHandle(CancellationController cancelController) {
            ExceptionHelper.checkNotNullArgument(cancelController, "cancelController");

            this.handleRef = new AtomicReference<>(null);
            this.cancelController = cancelController;
        }

        public void start(String displayName) {
            ProgressHandle newHandle = ProgressHandleFactory.createHandle(displayName, new Cancellable() {
                @Override
                public boolean cancel() {
                    cancelController.cancel();
                    return true;
                }
            });

            newHandle.start();

            ProgressHandle prevHandle = handleRef.getAndSet(newHandle);
            if (prevHandle != null) {
                prevHandle.finish();
            }
        }

        public void finish() {
            ProgressHandle prevRef = handleRef.getAndSet(null);
            if (prevRef != null) {
                prevRef.finish();
            }
        }
    }

    private GradleDaemonManager() {
        throw new AssertionError();
    }
}
