package org.netbeans.gradle.project.tasks.vars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.GradleTaskVariableQuery;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.openide.util.Lookup;

public final class TaskVariableMaps {
    public static TaskVariableMap getGlobalVariableMap() {
        return EnvTaskVariableMap.DEFAULT;
    }

    public static TaskVariableMap createProjectActionVariableMap(NbGradleProject project, Lookup actionContext) {
        final List<TaskVariableMap> maps = new ArrayList<>();

        addAsTaskVariableMap(project.getCommonProperties().customVariables().getActiveSource(), maps);

        Collection<? extends GradleTaskVariableQuery> taskVariables
                = project.getExtensions().lookupAllExtensionObjs(GradleTaskVariableQuery.class);
        for (GradleTaskVariableQuery query: taskVariables) {
            maps.add(query.getVariableMap(actionContext));
        }

        // Allow extensions to redefine variables.
        maps.add(StandardTaskVariable.createVarReplaceMap(project, actionContext));
        maps.add(getGlobalVariableMap());

        return new CombinedTaskVariableMap(maps);
    }

    private static TaskVariableMap asTaskVariableMap(PropertySource<? extends CustomVariables> varsProperty) {
        final CustomVariables vars = varsProperty.getValue();
        if (vars == null || vars.isEmpty()) {
            return null;
        }

        return (TaskVariable variable) -> vars.tryGetValue(variable.getVariableName());
    }

    private static void addAsTaskVariableMap(
            PropertySource<? extends CustomVariables> varsProperty,
            List<? super TaskVariableMap> result) {
        TaskVariableMap vars = asTaskVariableMap(varsProperty);
        if (vars != null) {
            result.add(vars);
        }
    }

    private TaskVariableMaps() {
        throw new AssertionError();
    }
}
