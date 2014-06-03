package org.netbeans.gradle.project.properties2.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

public final class CustomTasksProperty {
    private static final PropertyDef<CustomTasks, CustomTasks> PROPERTY_DEF = createPropertyDef();
    private static final String CONFIG_KEY_COMMON_TASKS = "common-tasks";
    private static final String CONFIG_KEY_TASK = "task";
    private static final String CONFIG_KEY_DISPLAY_NAME = "display-name";
    private static final String CONFIG_KEY_NON_BLOCKING = "non-blocking";
    private static final String CONFIG_KEY_TASK_NAMES = "task-names";
    private static final String CONFIG_KEY_TASK_NAME = "name";
    private static final String CONFIG_KEY_TASK_MUST_EXIST = "#attr-must-exist";
    private static final String CONFIG_KEY_TASK_ARGS = "task-args";
    private static final String CONFIG_KEY_JVM_ARGS = "task-jvm-args";
    private static final String CONFIG_KEY_ARG = "arg";
    private static final String VALUE_YES = "yes";
    private static final String VALUE_NO = "no";

    public static PropertySource<CustomTasks> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(ConfigPath.fromKeys(CONFIG_KEY_COMMON_TASKS));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<CustomTasks, CustomTasks> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static PropertyDef<CustomTasks, CustomTasks> createPropertyDef() {
        PropertyDef.Builder<CustomTasks, CustomTasks> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getKeyEncodingDef());
        result.setValueDef(CommonProperties.<CustomTasks>getIdentityValueDef());
        result.setValueMerger(getValueMerger());
        return result.create();
    }

    private static String boolToConfig(boolean value) {
        return value ? VALUE_YES : VALUE_NO;
    }

    private static boolean getBooleanValueDefaultFalse(ConfigTree config) {
        return VALUE_YES.equalsIgnoreCase(config.getValue(""));
    }

    private static PredefinedTask.Name decodeName(ConfigTree nameNode) {
        String name = nameNode.getValue("");
        boolean mustExist = getBooleanValueDefaultFalse(nameNode.getChildTree(CONFIG_KEY_TASK_MUST_EXIST));
        return new PredefinedTask.Name(name, mustExist);
    }

    private static List<String> parseArgs(ConfigTree argRoot) {
        List<ConfigTree> argNodes = argRoot.getChildTrees(CONFIG_KEY_ARG);
        List<String> result = new ArrayList<>(argNodes.size());
        for (ConfigTree argNode: argNodes) {
            result.add(argNode.getValue(""));
        }
        return result;
    }

    private static PredefinedTask decodeTask(ConfigTree config) {
        String displayName = config.getChildTree(CONFIG_KEY_DISPLAY_NAME).getValue("?");
        boolean nonBlocking = getBooleanValueDefaultFalse(config.getChildTree(CONFIG_KEY_NON_BLOCKING));

        List<ConfigTree> nameNodes = config
                .getChildTree(CONFIG_KEY_TASK_NAMES)
                .getChildTrees(CONFIG_KEY_TASK_NAME);
        List<PredefinedTask.Name> taskNames = new ArrayList<>(nameNodes.size());
        for (ConfigTree nameNode: nameNodes) {
            taskNames.add(decodeName(nameNode));
        }

        List<String> taskArgs = parseArgs(config.getChildTree(CONFIG_KEY_TASK_ARGS));
        List<String> jvmArgs = parseArgs(config.getChildTree(CONFIG_KEY_JVM_ARGS));
        return new PredefinedTask(displayName, taskNames, taskArgs, jvmArgs, nonBlocking);
    }


    public static List<PredefinedTask> decodeTaskList(ConfigTree config) {
        List<ConfigTree> taskNodes = config.getChildTrees(CONFIG_KEY_TASK);
        List<PredefinedTask> result = new ArrayList<>(taskNodes.size());

        for (ConfigTree taskNode: taskNodes) {
            result.add(decodeTask(taskNode));
        }
        return result;
    }

    private static CustomTasks decodeConfig(ConfigTree config) {
        return new CustomTasks(decodeTaskList(config));
    }

    private static void encodeTaskName(PredefinedTask.Name name, ConfigTree.Builder result) {
        result.getChildBuilder(CONFIG_KEY_TASK_MUST_EXIST).setValue(boolToConfig(name.isMustExist()));
        result.setValue(name.getName());
    }

    private static void encodeArgs(List<String> args, ConfigTree.Builder result) {
        for (String arg: args) {
            result.addChildBuilder(CONFIG_KEY_ARG).setValue(arg);
        }
    }

    private static void encodeTask(PredefinedTask task, ConfigTree.Builder result) {
        result.getChildBuilder(CONFIG_KEY_DISPLAY_NAME).setValue(task.getDisplayName());
        result.getChildBuilder(CONFIG_KEY_NON_BLOCKING).setValue(boolToConfig(task.isNonBlocking()));

        ConfigTree.Builder rootNameBuilder = result.getChildBuilder(CONFIG_KEY_TASK_NAMES);
        for (PredefinedTask.Name name: task.getTaskNames()) {
            ConfigTree.Builder nameBuilder = rootNameBuilder.addChildBuilder(CONFIG_KEY_TASK_NAME);
            encodeTaskName(name, nameBuilder);
        }

        encodeArgs(task.getArguments(), result.getChildBuilder(CONFIG_KEY_TASK_ARGS));
        encodeArgs(task.getJvmArguments(), result.getChildBuilder(CONFIG_KEY_JVM_ARGS));
    }

    private static ConfigTree encodeConfig(CustomTasks tasks) {
        ConfigTree.Builder result = new ConfigTree.Builder();
        for (PredefinedTask task: tasks.getTasks()) {
            ConfigTree.Builder taskBuilder = result.addChildBuilder(CONFIG_KEY_TASK);
            encodeTask(task, taskBuilder);
        }
        return result.create();
    }

    private static PropertyKeyEncodingDef<CustomTasks> getKeyEncodingDef() {
        return new PropertyKeyEncodingDef<CustomTasks>() {
            @Override
            public CustomTasks decode(ConfigTree config) {
                return decodeConfig(config);
            }

            @Override
            public ConfigTree encode(CustomTasks value) {
                return encodeConfig(value);
            }
        };
    }

    private static ValueMerger<CustomTasks> getValueMerger() {
        return new ValueMerger<CustomTasks>() {
            @Override
            public CustomTasks mergeValues(CustomTasks child, ValueReference<CustomTasks> parent) {
                if (child == null) {
                    return parent.getValue();
                }

                CustomTasks parentValue = parent.getValue();
                if (parentValue == null) {
                    return child;
                }

                return new CustomTasks(CollectionsEx.viewConcatList(child.getTasks(), parentValue.getTasks()));
            }
        };
    }

    private CustomTasksProperty() {
        throw new AssertionError();
    }
}
