package org.netbeans.gradle.project.properties2.standard;

import java.util.Arrays;
import java.util.List;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

import static org.netbeans.gradle.project.properties2.standard.CustomTasksProperty.getKeyEncodingDef;

public final class BuiltInTasksProperty {
    private static final PropertyDef<PredefinedTasks, BuiltInTasks> PROPERTY_DEF = createPropertyDef();
    private static final String CONFIG_KEY_BUILT_IN_TASKS = "built-in-tasks";

    public static PropertySource<BuiltInTasks> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(ConfigPath.fromKeys(CONFIG_KEY_BUILT_IN_TASKS));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<PredefinedTasks, BuiltInTasks> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<PredefinedTasks, BuiltInTasks> createPropertyDef() {
        PropertyDef.Builder<PredefinedTasks, BuiltInTasks> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getKeyEncodingDef());
        result.setValueDef(getValueDef());
        result.setValueMerger(getValueMerger());
        return result.create();
    }

    private static PropertyValueDef<PredefinedTasks, BuiltInTasks> getValueDef() {
        return new PropertyValueDef<PredefinedTasks, BuiltInTasks>() {
            @Override
            public PropertySource<BuiltInTasks> property(PredefinedTasks valueKey) {
                return PropertyFactory.constSource(valueKey != null ? new BuiltInTasks(valueKey) : null);
            }

            @Override
            public PredefinedTasks getKeyFromValue(BuiltInTasks value) {
                return value != null ? value.getPredefinedTasks() : null;
            }
        };
    }

    private static ValueMerger<BuiltInTasks> getValueMerger() {
        return new ValueMerger<BuiltInTasks>() {
            @Override
            public BuiltInTasks mergeValues(BuiltInTasks child, ValueReference<BuiltInTasks> parent) {
                if (child == null) {
                    return parent.getValue();
                }

                BuiltInTasks parentValue = parent.getValue();
                if (parentValue == null) {
                    return child;
                }

                return child.inheritFrom(parentValue);
            }
        };
    }

    private BuiltInTasksProperty() {
        throw new AssertionError();
    }
}
