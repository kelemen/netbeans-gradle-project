package org.netbeans.gradle.project.tasks.vars;

import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public final class EnvTaskVariableMap implements TaskVariableMap {
    private static final String ENV_PREFIX = "env.";

    public static final EnvTaskVariableMap DEFAULT = new EnvTaskVariableMap();

    @Override
    public String tryGetValueForVariable(TaskVariable variable) {
        String name = variable.getVariableName();
        if (!name.startsWith(ENV_PREFIX)) {
            return null;
        }

        String envVarName = name.substring(ENV_PREFIX.length());
        return System.getenv(envVarName);
    }
}
