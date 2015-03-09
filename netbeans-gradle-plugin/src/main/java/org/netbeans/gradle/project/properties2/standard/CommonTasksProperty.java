package org.netbeans.gradle.project.properties2.standard;

import java.util.List;
import org.jtrim.collections.CollectionsEx;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

import static org.netbeans.gradle.project.properties2.standard.CustomTasksProperty.getKeyEncodingDef;

public final class CommonTasksProperty {
    private static final String CONFIG_KEY_BUILT_IN_TASKS = "common-tasks";

    public static final PropertyDef<PredefinedTasks, PredefinedTasks> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<PredefinedTasks, PredefinedTasks> createPropertyDef() {
        PropertyDef.Builder<PredefinedTasks, PredefinedTasks> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_BUILT_IN_TASKS));
        result.setKeyEncodingDef(getKeyEncodingDef());
        result.setValueDef(getValueDef());
        result.setValueMerger(getValueMerger());
        return result.create();
    }

    private static PropertyValueDef<PredefinedTasks, PredefinedTasks> getValueDef() {
        return CommonProperties.getIdentityValueDef();
    }

    private static ValueMerger<PredefinedTasks> getValueMerger() {
        return new ValueMerger<PredefinedTasks>() {
            @Override
            public PredefinedTasks mergeValues(PredefinedTasks child, ValueReference<PredefinedTasks> parent) {
                if (child == null) {
                    return parent.getValue();
                }

                PredefinedTasks parentValue = parent.getValue();
                if (parentValue == null) {
                    return child;
                }

                List<PredefinedTask> tasks1 = child.getTasks();
                List<PredefinedTask> tasks2 = child.getTasks();

                return new PredefinedTasks(CollectionsEx.viewConcatList(tasks1, tasks2));
            }
        };
    }

    private CommonTasksProperty() {
        throw new AssertionError();
    }
}
