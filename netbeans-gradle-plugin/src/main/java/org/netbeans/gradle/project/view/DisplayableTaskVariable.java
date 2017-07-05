package org.netbeans.gradle.project.view;

import java.io.File;
import java.util.Map;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.ValueGetter;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableDef;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableDefMap;
import org.netbeans.gradle.project.tasks.vars.CachingVariableMap.VariableValue;
import org.openide.util.Lookup;

public enum DisplayableTaskVariable {
    PROJECT_PATH("project.path", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        return new VariableValue(getSafeProjectPath(model));
    }),
    PROJECT_GROUP("project.group", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        ProjectId projectId = model.getProjectId();
        return new VariableValue(projectId.getGroup());
    }),
    PROJECT_NAME("project.name", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        return new VariableValue(getSafeProjectName(model));
    }),
    PROJECT_VERSION("project.version", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        ProjectId projectId = model.getProjectId();
        return new VariableValue(projectId.getVersion());
    }),
    PARENT_PATH("parent.path", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        return new VariableValue(getProjectPath(parent(model)));
    }),
    PARENT_GROUP("parent.group", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        ProjectId projectId = parentId(model);
        return new VariableValue(projectId != null ? projectId.getGroup() : "");
    }),
    PARENT_NAME("parent.name", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        return new VariableValue(getProjectName(parent(model)));
    }),
    PARENT_VERSION("parent.version", (TaskVariableMap variables, NbGradleModel model, Lookup actionContext) -> {
        ProjectId projectId = parentId(model);
        return new VariableValue(projectId != null ? projectId.getVersion() : "");
    });

    private static NbGradleProjectTree parent(NbGradleModel model) {
        return model.getProjectDef().getParentTree();
    }

    private static GenericProjectProperties parentProperties(NbGradleModel model) {
        NbGradleProjectTree parent = parent(model);
        return parent != null ? parent.getGenericProperties() : null;
    }

    private static ProjectId parentId(NbGradleModel model) {
        GenericProjectProperties properties = parentProperties(model);
        return properties != null ? properties.getProjectId() : null;
    }

    private static String getProjectName(NbGradleProjectTree model) {
        if (model == null) {
            return "";
        }

        ProjectId projectId = model.getGenericProperties().getProjectId();
        String name = projectId.getName();
        if (name.isEmpty()) {
            return model.getProjectDir().getName();
        }
        else {
            return name;
        }
    }

    private static String getSafeProjectName(NbGradleModel model) {
        if (model.isBuildSrc()) {
            File parentFile = model.getProjectDir().getParentFile();
            return parentFile != null ? parentFile.getName() : "?";
        }
        else {
            return getProjectName(model.getMainProject());
        }
    }

    private static String getProjectPath(NbGradleProjectTree model) {
        if (model == null) {
            return "";
        }

        String path = model.getProjectFullName();
        if (path.isEmpty() || path.equals(":")) {
            return getProjectName(model);
        }
        else {
            return path;
        }
    }

    private static String getSafeProjectPath(NbGradleModel model) {
        if (model.isBuildSrc()) {
            File parentFile = model.getProjectDir().getParentFile();
            return parentFile != null ? parentFile.getName() : "?";
        }
        else {
            return getProjectPath(model.getMainProject());
        }
    }

    private static final VariableDefMap<NbGradleModel> TASK_VARIABLE_MAP
            = createStandardMap();

    private static VariableDefMap<NbGradleModel> createStandardMap() {
        DisplayableTaskVariable[] variables = DisplayableTaskVariable.values();

        Map<TaskVariable, VariableDef<NbGradleModel>> result = CollectionUtils.newHashMap(variables.length);
        for (DisplayableTaskVariable variable: variables) {
            result.put(variable.getVariable(), variable.asVariableDef());
        }

        return result::get;
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
