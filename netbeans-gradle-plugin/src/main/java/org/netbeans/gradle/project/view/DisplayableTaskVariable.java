package org.netbeans.gradle.project.view;

import java.util.Map;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.tasks.CachingVariableMap;
import org.netbeans.gradle.project.tasks.CachingVariableMap.ValueGetter;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableDef;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableDefMap;
import org.netbeans.gradle.project.tasks.CachingVariableMap.VariableValue;
import org.openide.util.Lookup;

public enum DisplayableTaskVariable {
    PROJECT_PATH("project.path", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            String path = project.currentModel().getValue().getMainProject().getProjectFullName();
            if (path.isEmpty() || path.equals(":")) {
                return new VariableValue(getSafeProjectName(project));
            }
            else {
                return new VariableValue(path);
            }
        }
    }),
    PROJECT_GROUP("project.group", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            ProjectId projectId = project.currentModel().getValue().getProjectId();
            return new VariableValue(projectId.getGroup());
        }
    }),
    PROJECT_NAME("project.name", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            return new VariableValue(getSafeProjectName(project));
        }
    }),
    PROJECT_VERSION("project.version", new ValueGetter<NbGradleProject>() {
        @Override
        public VariableValue getValue(TaskVariableMap variables, NbGradleProject project, Lookup actionContext) {
            ProjectId projectId = project.currentModel().getValue().getProjectId();
            return new VariableValue(projectId.getVersion());
        }
    });

    private static String getSafeProjectName(NbGradleProject project) {
        ProjectId projectId = project.currentModel().getValue().getProjectId();
        String name = projectId.getName();
        if (name.isEmpty()) {
            return project.getProjectDirectory().getNameExt();
        }
        else {
            return name;
        }
    }

    private static final VariableDefMap<NbGradleProject> TASK_VARIABLE_MAP
            = createStandardMap();

    private static VariableDefMap<NbGradleProject> createStandardMap() {
        DisplayableTaskVariable[] variables = DisplayableTaskVariable.values();

        final Map<TaskVariable, VariableDef<NbGradleProject>> result
                = CollectionUtils.newHashMap(variables.length);

        for (DisplayableTaskVariable variable: variables) {
            result.put(variable.getVariable(), variable.asVariableDef());
        }

        return new VariableDefMap<NbGradleProject>() {
            @Override
            public VariableDef<NbGradleProject> tryGetDef(TaskVariable variable) {
                return result.get(variable);
            }
        };
    }

    public static TaskVariableMap createVarReplaceMap(
            NbGradleProject project, Lookup actionContext) {
        return new CachingVariableMap<>(TASK_VARIABLE_MAP, project, actionContext);
    }

    private final TaskVariable variable;
    private final ValueGetter<NbGradleProject> valueGetter;

    private DisplayableTaskVariable(String variableName, ValueGetter<NbGradleProject> valueGetter) {
        this.variable = new TaskVariable(variableName);
        this.valueGetter = valueGetter;
    }

    private VariableDef<NbGradleProject> asVariableDef() {
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
