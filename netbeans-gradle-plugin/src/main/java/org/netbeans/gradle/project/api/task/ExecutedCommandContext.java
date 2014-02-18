package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;

// TODO: Document
public final class ExecutedCommandContext {
    private final TaskVariableMap taskVariables;

    public ExecutedCommandContext(@Nonnull TaskVariableMap taskVariables) {
        if (taskVariables == null) throw new NullPointerException("taskVariables");

        this.taskVariables = taskVariables;
    }

    @Nonnull
    public TaskVariableMap getTaskVariables() {
        return taskVariables;
    }
}
