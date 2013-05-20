package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.spi.project.ActionProvider;

public final class BuiltInTasks {
    private static final Logger LOGGER = Logger.getLogger(BuiltInTasks.class.getName());

    public static final PredefinedTask DEFAULT_BUILD_TASK = new PredefinedTask(
            ActionProvider.COMMAND_BUILD,
            asTaskNames("build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_TEST_TASK = new PredefinedTask(
            ActionProvider.COMMAND_TEST,
            asTaskNames("cleanTest", "test"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_CLEAN_TASK = new PredefinedTask(
            ActionProvider.COMMAND_CLEAN,
            asTaskNames("clean"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_RUN_TASK = new PredefinedTask(
            ActionProvider.COMMAND_RUN,
            asTaskNames("run"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            false);
    public static final PredefinedTask DEFAULT_DEBUG_TASK = new PredefinedTask(
            ActionProvider.COMMAND_DEBUG,
            asTaskNames("debug"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            false);
    public static final PredefinedTask DEFAULT_JAVADOC_TASK = new PredefinedTask(
            JavaProjectConstants.COMMAND_JAVADOC,
            asTaskNames("javadoc"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_REBUILD_TASK = new PredefinedTask(
            ActionProvider.COMMAND_REBUILD,
            asTaskNames("clean", "build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_TEST_SINGLE_TASK = new PredefinedTask(
            ActionProvider.COMMAND_TEST_SINGLE,
            asTaskNames(projectTask("cleanTest"), projectTask("test")),
            Arrays.asList("-Dtest.single=${test-file-path}"),
            Collections.<String>emptyList(),
            true);
    public static final PredefinedTask DEFAULT_DEBUG_TEST_SINGLE_TASK = new PredefinedTask(
            ActionProvider.COMMAND_DEBUG_TEST_SINGLE,
            asTaskNames(projectTask("cleanTest"), projectTask("test")),
            Arrays.asList("-Dtest.single=" + StandardTaskVariable.TEST_FILE_PATH.getScriptReplaceConstant(), "-Dtest.debug"),
            Collections.<String>emptyList(),
            false);
    public static final PredefinedTask DEFAULT_RUN_SINGLE_TASK = new PredefinedTask(
            ActionProvider.COMMAND_RUN_SINGLE,
            asTaskNames(projectTask("run")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            Collections.<String>emptyList(),
            false);
    public static final PredefinedTask DEFAULT_DEBUG_SINGLE_TASK = new PredefinedTask(
            ActionProvider.COMMAND_DEBUG_SINGLE,
            asTaskNames(projectTask("debug")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            Collections.<String>emptyList(),
            false);
    public static final PredefinedTask DEFAULT_APPLY_CODE_CHANGES_TASK = new PredefinedTask(
            JavaProjectConstants.COMMAND_DEBUG_FIX,
            asTaskNames(projectTask("classes")),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            false);

    private static final Map<String, PredefinedTask> DEFAULT_TASKS;
    private static final Map<String, String> DISPLAY_NAME_MAP;

    static {
        DEFAULT_TASKS = new HashMap<String, PredefinedTask>();
        addToMap(DEFAULT_BUILD_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_TEST_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_CLEAN_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_RUN_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_DEBUG_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_JAVADOC_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_REBUILD_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_TEST_SINGLE_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_DEBUG_TEST_SINGLE_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_RUN_SINGLE_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_DEBUG_SINGLE_TASK, DEFAULT_TASKS);
        addToMap(DEFAULT_APPLY_CODE_CHANGES_TASK, DEFAULT_TASKS);

        DISPLAY_NAME_MAP = new HashMap<String, String>();
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_BUILD, NbStrings.getBuildCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST, NbStrings.getTestCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_CLEAN, NbStrings.getCleanCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_RUN, NbStrings.getRunCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG, NbStrings.getDebugCommandCaption());
        DISPLAY_NAME_MAP.put(JavaProjectConstants.COMMAND_JAVADOC, NbStrings.getJavadocCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_REBUILD, NbStrings.getRebuildCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST_SINGLE, NbStrings.getTestSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, NbStrings.getDebugTestSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_RUN_SINGLE, NbStrings.getRunSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG_SINGLE, NbStrings.getDebugSingleCommandCaption());
        DISPLAY_NAME_MAP.put(JavaProjectConstants.COMMAND_DEBUG_FIX, NbStrings.getApplyCodeChangesCommandCaption());
    }

    private static void addToMap(PredefinedTask task, Map<String, PredefinedTask> map) {
        map.put(task.getDisplayName(), task);
    }

    private static List<PredefinedTask.Name> asTaskNames(String... names) {
        List<PredefinedTask.Name> result = new ArrayList<PredefinedTask.Name>(names.length);
        for (String name: names) {
            result.add(new PredefinedTask.Name(name, false));
        }
        return result;
    }

    private static String projectTask(String task) {
        return StandardTaskVariable.PROJECT_NAME.getScriptReplaceConstant() + ":" + task;
    }

    public static String getDisplayNameOfCommand(String command) {
        if (command == null) throw new NullPointerException("command");

        String displayName = DISPLAY_NAME_MAP.get(command);
        if (displayName == null) {
            if (AbstractProjectProperties.getCustomizableCommands().contains(command)) {
                LOGGER.log(Level.WARNING, "Customizable command does not have a display name: {0}", command);
            }
            else {
                LOGGER.log(Level.WARNING, "Unknown command does not have a display name: {0}", command);
            }
            displayName = command;
        }
        return displayName;
    }

    public static PredefinedTask getDefaultBuiltInTask(String command) {
        PredefinedTask result = DEFAULT_TASKS.get(command);
        if (result == null) {
            LOGGER.log(Level.SEVERE, "Unknown built-in task: {0}", command);
            result = new PredefinedTask(
                    command,
                    asTaskNames(command),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    false);
        }
        return result;
    }

    private BuiltInTasks() {
        throw new AssertionError();
    }
}
