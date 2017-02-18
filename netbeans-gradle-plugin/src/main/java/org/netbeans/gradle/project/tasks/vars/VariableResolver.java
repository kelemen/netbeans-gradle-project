package org.netbeans.gradle.project.tasks.vars;

import java.util.Collection;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public interface VariableResolver {
    public String replaceVars(
            String str,
            TaskVariableMap varReplaceMap,
            Collection<? super DisplayedTaskVariable> collectedVariables);

    public String replaceVars(
            String str,
            TaskVariableMap varReplaceMap);

    public void collectVars(
            String str,
            TaskVariableMap varReplaceMap,
            Collection<? super DisplayedTaskVariable> collectedVariables);

    public String replaceVarsIfValid(
            String str,
            TaskVariableMap varReplaceMap);
}
