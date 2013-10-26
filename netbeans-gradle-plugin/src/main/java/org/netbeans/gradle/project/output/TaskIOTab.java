package org.netbeans.gradle.project.output;

import java.util.Arrays;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.openide.windows.InputOutput;

public final class TaskIOTab implements IOTabDef {
    private final InputOutputWrapper io;
    private final TaskTabAction[] actions;

    public TaskIOTab(InputOutput io, TaskTabAction... actions) {
        this.io = new InputOutputWrapper(io);
        this.actions = actions.clone();

        CollectionUtils.checkNoNullElements(Arrays.asList(this.actions), "actions");
    }

    public void setLastTask(GradleTaskDef source, AsyncGradleTask lastTask) {
        for (TaskTabAction action: actions) {
            action.setLastTask(source, lastTask);
        }
    }

    public void taskStarted() {
        for (TaskTabAction action: actions) {
            action.taskStarted();
        }
    }

    public void taskCompleted() {
        for (TaskTabAction action: actions) {
            action.taskCompleted();
        }
    }

    public InputOutputWrapper getIo() {
        return io;
    }

    @Override
    public boolean isClosed() {
        return io.getIo().isClosed();
    }
}
