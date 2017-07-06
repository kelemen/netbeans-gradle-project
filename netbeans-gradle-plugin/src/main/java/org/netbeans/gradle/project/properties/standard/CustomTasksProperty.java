package org.netbeans.gradle.project.properties.standard;

import java.util.ArrayList;
import java.util.List;
import org.jtrim2.collections.CollectionsEx;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.api.config.ValueReference;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class CustomTasksProperty {
    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("common-tasks");

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

    public static final PropertyDef<PredefinedTasks, PredefinedTasks> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<PredefinedTasks, PredefinedTasks> createPropertyDef() {
        PropertyDef.Builder<PredefinedTasks, PredefinedTasks> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(getKeyEncodingDef());
        result.setValueDef(getValueDef());
        result.setValueMerger(getValueMerger());
        return result.create();
    }

    private static String boolToConfig(boolean value) {
        return value ? VALUE_YES : VALUE_NO;
    }

    private static boolean getBooleanValueDefaultFalse(ConfigTree config) {
        return VALUE_YES.equalsIgnoreCase(config.getValue(""));
    }

    private static PropertyValueDef<PredefinedTasks, PredefinedTasks> getValueDef() {
        return CommonProperties.getIdentityValueDef();
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


    private static List<PredefinedTask> decodeTaskList(ConfigTree config) {
        List<ConfigTree> taskNodes = config.getChildTrees(CONFIG_KEY_TASK);
        List<PredefinedTask> result = new ArrayList<>(taskNodes.size());

        for (ConfigTree taskNode: taskNodes) {
            result.add(decodeTask(taskNode));
        }
        return result;
    }

    private static PredefinedTasks decodeConfig(ConfigTree config) {
        return new PredefinedTasks(decodeTaskList(config));
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

    private static ConfigTree encodeConfig(PredefinedTasks tasks) {
        ConfigTree.Builder result = new ConfigTree.Builder();
        for (PredefinedTask task: tasks.getTasks()) {
            ConfigTree.Builder taskBuilder = result.addChildBuilder(CONFIG_KEY_TASK);
            encodeTask(task, taskBuilder);
        }
        return result.create();
    }

    public static PropertyKeyEncodingDef<PredefinedTasks> getKeyEncodingDef() {
        return new PropertyKeyEncodingDef<PredefinedTasks>() {
            @Override
            public PredefinedTasks decode(ConfigTree config) {
                return decodeConfig(config);
            }

            @Override
            public ConfigTree encode(PredefinedTasks value) {
                return encodeConfig(value);
            }
        };
    }

    private static ValueMerger<PredefinedTasks> getValueMerger() {
        return (PredefinedTasks child, ValueReference<PredefinedTasks> parent) -> {
            if (child == null) {
                return parent.getValue();
            }

            PredefinedTasks parentValue = parent.getValue();
            if (parentValue == null) {
                return child;
            }

            List<PredefinedTask> tasks1 = child.getTasks();
            List<PredefinedTask> tasks2 = parentValue.getTasks();

            return new PredefinedTasks(CollectionsEx.viewConcatList(tasks1, tasks2));
        };
    }

    private CustomTasksProperty() {
        throw new AssertionError();
    }
}
