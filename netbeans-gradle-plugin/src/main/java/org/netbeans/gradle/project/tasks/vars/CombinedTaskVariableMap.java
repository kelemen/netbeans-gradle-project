package org.netbeans.gradle.project.tasks.vars;

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;

public final class CombinedTaskVariableMap implements TaskVariableMap {
    private final TaskVariableMap[] maps;

    public CombinedTaskVariableMap(Collection<? extends TaskVariableMap> maps) {
        this(maps.toArray(new TaskVariableMap[maps.size()]));
    }

    public CombinedTaskVariableMap(TaskVariableMap... maps) {
        this.maps = maps.clone();

        CollectionUtils.checkNoNullElements(Arrays.asList(this.maps), "maps");
    }

    @Override
    public String tryGetValueForVariable(TaskVariable variable) {
        for (TaskVariableMap map: maps) {
            String value = map.tryGetValueForVariable(variable);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
