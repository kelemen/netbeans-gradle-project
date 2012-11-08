package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.view.GradleActionProvider;
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
            GradleActionProvider.COMMAND_JAVADOC,
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
            Arrays.asList("-Dtest.single=" + PredefinedTask.VAR_TEST_FILE_PATH, "-Dtest.debug"),
            Collections.<String>emptyList(),
            true);

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

        DISPLAY_NAME_MAP = new HashMap<String, String>();
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_BUILD, NbStrings.getBuildCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST, NbStrings.getTestCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_CLEAN, NbStrings.getCleanCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_RUN, NbStrings.getRunCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG, NbStrings.getDebugCommandCaption());
        DISPLAY_NAME_MAP.put(GradleActionProvider.COMMAND_JAVADOC, NbStrings.getJavadocCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_REBUILD, NbStrings.getRebuildCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST_SINGLE, NbStrings.getTestSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, NbStrings.getDebugTestSingleCommandCaption());
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
        return PredefinedTask.VAR_PROJECT_NAME + ":" + task;
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
