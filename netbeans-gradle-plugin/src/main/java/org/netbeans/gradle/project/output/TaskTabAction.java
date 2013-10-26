package org.netbeans.gradle.project.output;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;
import org.netbeans.gradle.project.tasks.GradleTaskDef;

@SuppressWarnings("serial")
public abstract class TaskTabAction extends AbstractAction {
    private volatile GradleTaskDef lastSourceTask;
    private volatile AsyncGradleTask lastTask;

    public TaskTabAction() {
        this.lastTask = null;
    }

    protected void taskStarted() {
        setEnableAction(false);
    }

    protected void taskCompleted() {
        setEnableAction(true);
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setEnabled(enabled);
            }
        });
    }
}
