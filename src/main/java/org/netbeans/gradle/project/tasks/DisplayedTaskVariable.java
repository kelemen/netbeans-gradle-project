package org.netbeans.gradle.project.tasks;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public final class DisplayedTaskVariable {
    private final TaskVariable variable;
    private final String displayName;

    public DisplayedTaskVariable(String variableName) {
        this(new TaskVariable(variableName));
    }

    public DisplayedTaskVariable(String variableName, String displayName) {
        this(new TaskVariable(variableName), displayName);
    }

    public DisplayedTaskVariable(TaskVariable variable) {
        this(variable, variable.getVariableName());
    }

    public DisplayedTaskVariable(TaskVariable variable, String displayName) {
        if (variable == null) throw new NullPointerException("variable");
        if (displayName == null) throw new NullPointerException("displayName");

        this.variable = variable;
        this.displayName = displayName;
    }

    public TaskVariable getVariable() {
        return variable;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getScriptReplaceConstant() {
        String varName = variable.getVariableName();
        if (varName.equals(displayName)) {
            return variable.getScriptReplaceConstant();
        }
        else {
            return "${" + variable.getVariableName() + ":" + displayName + "}";
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + variable.hashCode();
        hash = 97 * hash + displayName.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final DisplayedTaskVariable other = (DisplayedTaskVariable)obj;

        return this.variable.equals(other.variable)
                && this.displayName.equals(other.displayName);
    }

    public static TaskVariableMap variableMap(Map<DisplayedTaskVariable, String> map) {
        final Map<TaskVariable, String> appliedMap = new HashMap<TaskVariable, String>();
        for (Map.Entry<DisplayedTaskVariable, String> entry: map.entrySet()) {
            appliedMap.put(entry.getKey().getVariable(), entry.getValue());
        }

        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                return appliedMap.get(variable);
            }
        };
    }
}
