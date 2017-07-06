package org.netbeans.gradle.project.output;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationSource;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;
import org.netbeans.gradle.project.tasks.GradleTaskDef;

@SuppressWarnings("serial")
public abstract class TaskTabAction extends AbstractAction {
    private final boolean enableWhileRunning;
    private volatile GradleTaskDef lastSourceTask;
    private volatile AsyncGradleTask lastTask;
    private volatile CancellationSource lastCancellation;

    public TaskTabAction() {
        this(false);
    }

    public TaskTabAction(boolean enableWhileRunning) {
        this.enableWhileRunning = enableWhileRunning;
        this.lastSourceTask = null;
        this.lastTask = null;
        this.lastCancellation = null;

        if (enableWhileRunning) {
            setEnabled(false);
        }
    }

    protected void taskStarted(CancellationSource cancellation) {
        lastCancellation = cancellation;
        setEnableAction(enableWhileRunning);
    }

    protected void taskCompleted() {
        setEnableAction(!enableWhileRunning);
        lastCancellation = null; // Do not refernece it needlessly
    }

    protected final void cancelCurrentlyRunning() {
        CancellationSource currentCancellation = lastCancellation;
        if (currentCancellation != null) {
            currentCancellation.getController().cancel();
        }
    }

    protected final void setLastTask(GradleTaskDef lastSourceTask, AsyncGradleTask lastTask) {
        this.lastSourceTask = lastSourceTask;
        this.lastTask = lastTask;
    }

    protected final GradleTaskDef getLastSourceTask() {
        return lastSourceTask;
    }

    protected final AsyncGradleTask getLastTask() {
        return lastTask;
    }

    public final void setEnableAction(final boolean enabled) {
        SwingUtilities.invokeLater(() -> setEnabled(enabled));
    }
}
