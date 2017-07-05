package org.netbeans.gradle.project.tasks.vars;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public final class DisplayedTaskVariable {
    private final TaskVariable variable;
    private final String displayName;
    private final VariableTypeDescription typeDescription;

    public DisplayedTaskVariable(
            TaskVariable variable,
            String displayName,
            VariableTypeDescription typeDescription) {

        ExceptionHelper.checkNotNullArgument(variable, "variable");
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");
        ExceptionHelper.checkNotNullArgument(typeDescription, "typeDescription");

        this.variable = variable;
        this.displayName = displayName;
        this.typeDescription = typeDescription;
    }

    public TaskVariable getVariable() {
        return variable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VariableTypeDescription getTypeDescription() {
        return typeDescription;
    }

    public boolean isDefault() {
        return variable.getVariableName().equals(displayName)
                && typeDescription.isDefault();
    }

    @Nonnull
    public String getScriptReplaceConstant() {
        return LenientVariableResolver.getScriptReplaceConstant(this);
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

    @Override
    public String toString() {
        return getScriptReplaceConstant();
    }

    public static TaskVariableMap variableMap(Map<DisplayedTaskVariable, String> map) {
        Map<TaskVariable, String> appliedMap = new HashMap<>();
        for (Map.Entry<DisplayedTaskVariable, String> entry: map.entrySet()) {
            appliedMap.put(entry.getKey().getVariable(), entry.getValue());
        }

        return appliedMap::get;
    }
}
