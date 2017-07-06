package org.netbeans.gradle.project.tasks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationController;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.executor.TaskExecutor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;

public final class GradleDaemonManager {
    private static final Logger LOGGER = Logger.getLogger(GradleDaemonManager.class.getName());

    private static final ReentrantLock QUEUE_LOCK = new ReentrantLock(true);

    private static void runNonBlockingGradleTask(
            CancellationToken cancelToken,
            DaemonTask task,
            ProgressHandle progress) {

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
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(taskDefFactory, "taskDefFactory");
        Objects.requireNonNull(listener, "listener");

        final CancellationSource cancel = Cancellation.createCancellationSource();
        final String origDisplayName = taskDefFactory.getDisplayName();

        final ReplaceableProgressHandle progress = new ReplaceableProgressHandle(cancel.getController());
        final AtomicBoolean inProgress = new AtomicBoolean(false);

        cancel.getToken().addCancellationListener(() -> {
            if (!inProgress.get()) {
                progress.finish();
            }
        });

        progress.start(origDisplayName);
        executor.execute(cancel.getToken(), (CancellationToken cancelToken) -> {
            inProgress.set(true);
            cancelToken.checkCanceled();

            DaemonTaskDef taskDef;
            try {
                taskDef = taskDefFactory.tryCreateTaskDef(cancelToken);
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

            if (nonBlocking) {
                runNonBlockingGradleTask(cancelToken, task, progress.getCurrentHandle());
            }
            else {
                runBlockingGradleTask(cancelToken, task, progress.getCurrentHandle());
            }
        }).handle((result, error) -> {
            if (AsyncTasks.isCanceled(error)) {
                LOGGER.log(Level.INFO, "Canceled task: {0}", origDisplayName);
            }
            else {
                listener.onComplete(error);
            }
            return null;
        }).whenComplete((result, error) -> {
            progress.finish();
        }).exceptionally(AsyncTasks::expectNoError);
    }

    private static final class ReplaceableProgressHandle {
        private final AtomicReference<ProgressHandle> handleRef;
        private final CancellationController cancelController;

        public ReplaceableProgressHandle(CancellationController cancelController) {
            this.handleRef = new AtomicReference<>(null);
            this.cancelController = Objects.requireNonNull(cancelController, "cancelController");
        }

        public void start(String displayName) {
            ProgressHandle newHandle = ProgressHandle.createHandle(displayName, () -> {
                cancelController.cancel();
                return true;
            });

            newHandle.start();
            newHandle.suspend("");

            ProgressHandle prevHandle = handleRef.getAndSet(newHandle);
            if (prevHandle != null) {
                prevHandle.finish();
            }
        }

        public ProgressHandle getCurrentHandle() {
            return handleRef.get();
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
