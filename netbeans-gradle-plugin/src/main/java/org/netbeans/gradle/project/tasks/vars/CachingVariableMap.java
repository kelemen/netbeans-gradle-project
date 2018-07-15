package org.netbeans.gradle.project.tasks.vars;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.openide.util.Lookup;

public final class CachingVariableMap<ProjectInfo> implements TaskVariableMap {
    public interface ValueGetter<ProjectInfo> {
        public abstract VariableValue getValue(
                TaskVariableMap variables,
                ProjectInfo project,
                Lookup actionContext);
    }

    public interface VariableDefMap<ProjectInfo> {
        public VariableDef<ProjectInfo> tryGetDef(TaskVariable variable);
    }

    public static final class VariableDef<ProjectInfo> {
        private final TaskVariable variable;
        private final ValueGetter<ProjectInfo> valueGetter;

        public VariableDef(TaskVariable variable, ValueGetter<ProjectInfo> valueGetter) {
            this.variable = variable;
            this.valueGetter = valueGetter;
        }

        public TaskVariable getVariable() {
            return variable;
        }

        public ValueGetter<ProjectInfo> getValueGetter() {
            return valueGetter;
        }

        public VariableValue tryGetValue(TaskVariableMap variables, ProjectInfo project, Lookup actionContext) {
            return valueGetter.getValue(variables, project, actionContext);
        }
    }

    public static final class VariableValue {
        public static final VariableValue NULL_VALUE = new VariableValue(null);
        public static final VariableValue EMPTY_VALUE = new VariableValue("");

        private final String value;

        public VariableValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final ProjectInfo project;
    private final Lookup actionContext;
    private final VariableDefMap<ProjectInfo> taskVariableMap;

    private final ConcurrentMap<TaskVariable, VariableValue> cache;

    public CachingVariableMap(
            VariableDefMap<ProjectInfo> taskVariableMap,
            ProjectInfo project,
            Lookup actionContext) {
        this.project = Objects.requireNonNull(project, "project");
        this.actionContext = Objects.requireNonNull(actionContext, "actionContext");
        this.taskVariableMap = Objects.requireNonNull(taskVariableMap, "taskVariableMap");
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public String tryGetValueForVariable(TaskVariable variable) {
        VariableDef<ProjectInfo> var = taskVariableMap.tryGetDef(variable);
        if (var == null) {
            return null;
        }

        VariableValue result = cache.get(variable);
        if (result == null) {
            result = var.tryGetValue(this, project, actionContext);

            VariableValue prevResult = cache.putIfAbsent(variable, result);
            if (prevResult != null) {
                result = prevResult;
            }
        }
        return result.value;
    }
}
