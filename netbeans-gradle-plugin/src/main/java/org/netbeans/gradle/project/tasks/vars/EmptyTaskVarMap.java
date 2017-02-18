package org.netbeans.gradle.project.tasks.vars;

import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public enum EmptyTaskVarMap implements TaskVariableMap {
    INSTANCE;

    @Override
    public String tryGetValueForVariable(TaskVariable variable) {
        return null;
    }
}
