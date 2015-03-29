package org.netbeans.gradle.project.view;

import java.util.Map;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.tasks.CachingVariableMap;
import org.netbeans.gradle.project.tasks.CachingVariableMap.ValueGetter;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableDef;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableDefMap;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableValue;
import org.openide.util.Lookup;

public enum DisplayableTaskVariable {
    PROJECT_PATH("project.path", new ValueGetter<NbGradleModel>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleModel model, Lookup actionContext) {
            String path = model.getMainProject().getProjectFullName();
            if (path.isEmpty() || path.equals(":")) {
                return new VariableValue(getSafeProjectName(model));
            }
            else {
                return new VariableValue(path);
            }
        }
    }),
    PROJECT_GROUP("project.group", new ValueGetter<NbGradleModel>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleModel model, Lookup actionContext) {
            ProjectId projectId = model.getProjectId();
            return new VariableValue(projectId.getGroup());
        }
    }),
    PROJECT_NAME("project.name", new ValueGetter<NbGradleModel>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleModel model, Lookup actionContext) {
            return new VariableValue(getSafeProjectName(model));
        }
    }),
    PROJECT_VERSION("project.version", new ValueGetter<NbGradleModel>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleModel model, Lookup actionContext) {
            ProjectId projectId = model.getProjectId();
            return new VariableValue(projectId.getVersion());
        }
    });

    private static String getSafeProjectName(NbGradleModel model) {
        ProjectId projectId = model.getProjectId();
        String name = projectId.getName();
        if (name.isEmpty()) {
            return model.getProjectDir().getName();
        }
        else {
            return name;
        }
    }

    private static final VariableDefMap<NbGradleModel> TASK_VARIABLE_MAP
            = createStandardMap();

    private static VariableDefMap<NbGradleModel> createStandardMap() {
        DisplayableTaskVariable[] variables = DisplayableTaskVariable.values();

        final Map<TaskVariable, VariableDef<NbGradleModel>> result
                = CollectionUtils.newHashMap(variables.length);

        for (DisplayableTaskVariable variable: variables) {
            result.put(variable.getVariable(), variable.asVariableDef());
        }

        return new VariableDefMap<NbGradleModel>() {
            @Override
            public VariableDef<NbGradleModel> tryGetDef(TaskVariable variable) {
                return result.get(variable);
            }
        };
    }

    public static TaskVariableMap createVarReplaceMap(NbGradleModel project) {
        return new CachingVariableMap<>(TASK_VARIABLE_MAP, project, Lookup.EMPTY);
    }

    private final TaskVariable variable;
    private final ValueGetter<NbGradleModel> valueGetter;

    private DisplayableTaskVariable(String variableName, ValueGetter<NbGradleModel> valueGetter) {
        this.variable = new TaskVariable(variableName);
        this.valueGetter = valueGetter;
    }

    private VariableDef<NbGradleModel> asVariableDef() {
        return new VariableDef<>(variable, valueGetter);
    }

    public TaskVariable getVariable() {
        return variable;
    }

    public String getVariableName() {
        return variable.getVariableName();
    }

    public String getScriptReplaceConstant() {
        return variable.getScriptReplaceConstant();
    }
}
