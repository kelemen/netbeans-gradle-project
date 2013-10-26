package org.netbeans.gradle.project.output;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;

@SuppressWarnings("serial")
public abstract class TaskTabAction extends AbstractAction {
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

    protected final void setLastTask(AsyncGradleTask lastTask) {
        this.lastTask = lastTask;
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
