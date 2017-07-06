package org.netbeans.gradle.project.properties.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.api.config.ValueReference;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class BuiltInTasksProperty {
    private static final ConfigPath CONFIG_ROOT_BUILT_IN_TASKS = ConfigPath.fromKeys("built-in-tasks");

    public static final PropertyDef<?, BuiltInTasks> PROPERTY_DEF = createPropertyDef();

    public static BuiltInTasks createValue(Collection<? extends PredefinedTask> tasks) {
        return createValue(new PredefinedTasks(tasks));
    }

    public static BuiltInTasks createValue(PredefinedTasks tasks) {
        return new BuiltInTasksImpl(tasks);
    }

    public static PropertySource<BuiltInTasks> defaultValue(
            final NbGradleProject project,
            final ActiveSettingsQuery settingsQuery) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(settingsQuery, "settingsQuery");

        return new PropertySource<BuiltInTasks>() {
            @Override
            public BuiltInTasks getValue() {
                return builtInTasks(project, settingsQuery);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return settingsQuery.currentProfileSettings().addChangeListener(listener);
            }
        };
    }

    private static BuiltInTasks builtInTasks(
            final NbGradleProject project,
            final ActiveSettingsQuery settingsQuery) {
        return new BuiltInTasks() {
            @Override
            public PredefinedTasks getAllTasks() {
                return collectAllTasks(project, settingsQuery);
            }

            @Override
            public PredefinedTask tryGetByCommand(String command) {
                ProfileDef key = getProfileDef(settingsQuery);

                GradleCommandTemplate commandTemplate
                        = project.getMergedCommandQuery().tryGetDefaultGradleCommand(key, command);
                return commandTemplate != null
                        ? templateToPredefined(command, commandTemplate)
                        : null;
            }
        };
    }

    private static PredefinedTasks collectAllTasks(
            NbGradleProject project,
            ActiveSettingsQuery settingsQuery) {
        ProfileDef key = getProfileDef(settingsQuery);
        BuiltInGradleCommandQuery query = project.getMergedCommandQuery();

        Set<String> supportedCommands = query.getSupportedCommands();
        List<PredefinedTask> result = new ArrayList<>(supportedCommands.size());
        for (String command: supportedCommands) {
            GradleCommandTemplate commandTemplate
                    = query.tryGetDefaultGradleCommand(key, command);
            if (commandTemplate != null) {
                result.add(templateToPredefined(command, commandTemplate));
            }
        }

        return new PredefinedTasks(result);
    }

    private static ProfileDef getProfileDef(ActiveSettingsQuery settingsQuery) {
        SingleProfileSettings settings = settingsQuery.currentProfileSettings().getValue();
        ProfileKey key = settings != null ? settings.getKey() : null;
        return key != null
                ? new ProfileDef(key, key.getFileName())
                : null;
    }

    private static PredefinedTask templateToPredefined(
            String displayName, GradleCommandTemplate command) {
        List<String> tasks = command.getTasks();
        List<PredefinedTask.Name> taskNames = new ArrayList<>(tasks.size());
        for (String taskName: command.getTasks()) {
            taskNames.add(new PredefinedTask.Name(taskName, false));
        }

        return new PredefinedTask(displayName,
                taskNames,
                command.getArguments(),
                command.getJvmArguments(),
                !command.isBlocking());
    }

    private static PropertyDef<?, BuiltInTasks> createPropertyDef() {
        PropertyDef.Builder<PredefinedTasks, BuiltInTasks> result
                = new PropertyDef.Builder<>(CONFIG_ROOT_BUILT_IN_TASKS);
        result.setKeyEncodingDef(CustomTasksProperty.getKeyEncodingDef());
        result.setValueDef(getValueDef());
        result.setValueMerger(getValueMerger());
        return result.create();
    }

    private static PropertyValueDef<PredefinedTasks, BuiltInTasks> getValueDef() {
        return new PropertyValueDef<PredefinedTasks, BuiltInTasks>() {
            @Override
            public PropertySource<BuiltInTasks> property(PredefinedTasks valueKey) {
                BuiltInTasks value = valueKey != null
                        ? new BuiltInTasksImpl(valueKey)
                        : null;
                return PropertyFactory.<BuiltInTasks>constSource(value);
            }

            @Override
            public PredefinedTasks getKeyFromValue(BuiltInTasks value) {
                return value != null ? value.getAllTasks() : null;
            }
        };
    }

    private static ValueMerger<BuiltInTasks> getValueMerger() {
        return (BuiltInTasks child, ValueReference<BuiltInTasks> parent) -> {
            if (child == null) {
                return parent.getValue();
            }

            BuiltInTasks parentValue = parent.getValue();
            if (parentValue == null) {
                return child;
            }

            return new InheritingBuiltInTask(child, parentValue);
        };
    }

    private static final class InheritingBuiltInTask implements BuiltInTasks {
        private final BuiltInTasks tasks1;
        private final BuiltInTasks tasks2;

        public InheritingBuiltInTask(BuiltInTasks tasks1, BuiltInTasks tasks2) {
            assert tasks1 != null;
            assert tasks2 != null;

            this.tasks1 = tasks1;
            this.tasks2 = tasks2;
        }

        @Override
        public PredefinedTasks getAllTasks() {
            List<PredefinedTask> taskList1 = tasks1.getAllTasks().getTasks();
            List<PredefinedTask> taskList2 = tasks2.getAllTasks().getTasks();

            return new PredefinedTasks(CollectionsEx.viewConcatList(taskList1, taskList2));
        }

        @Override
        public PredefinedTask tryGetByCommand(String command) {
            PredefinedTask result = tasks1.tryGetByCommand(command);
            if (result == null) {
                result = tasks2.tryGetByCommand(command);
            }
            return result;
        }
    }

    private static final class BuiltInTasksImpl implements BuiltInTasks {
        private final PredefinedTasks tasks;
        private final Map<String, PredefinedTask> nameToTask;

        private BuiltInTasksImpl(PredefinedTasks tasks) {
            this.tasks = Objects.requireNonNull(tasks, "tasks");
            this.nameToTask = createNameToTask(tasks.getTasks());
        }

        private static Map<String, PredefinedTask> createNameToTask(Collection<PredefinedTask> tasks) {
            Map<String, PredefinedTask> result = CollectionsEx.newHashMap(tasks.size());
            for (PredefinedTask task: tasks) {
                result.put(task.getDisplayName(), task);
            }

            return Collections.unmodifiableMap(result);
        }

        @Override
        public PredefinedTask tryGetByCommand(String command) {
            Objects.requireNonNull(command, "command");
            return nameToTask.get(command);
        }

        @Override
        public PredefinedTasks getAllTasks() {
            return tasks;
        }
    }

    private BuiltInTasksProperty() {
        throw new AssertionError();
    }
}
