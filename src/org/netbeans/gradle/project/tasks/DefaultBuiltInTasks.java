package org.netbeans.gradle.project.tasks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.spi.project.ActionProvider;

// FIXME: We do not consider skip test global property.

public final class DefaultBuiltInTasks implements BuiltInGradleCommandQuery {
    private static final Logger LOGGER = Logger.getLogger(DefaultBuiltInTasks.class.getName());

    private static final CommandWithActions DEFAULT_BUILD_TASK = nonBlockingCommand(
            Arrays.asList("build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_TEST_TASK = nonBlockingCommand(
            Arrays.asList("cleanTest", "test"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_CLEAN_TASK = nonBlockingCommand(
            Arrays.asList("clean"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_RUN_TASK = blockingCommand(
            Arrays.asList("run"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.RUN);
    private static final CommandWithActions DEFAULT_DEBUG_TASK = blockingCommand(
            Arrays.asList("debug"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.DEBUG);
    private static final CommandWithActions DEFAULT_JAVADOC_TASK = nonBlockingCommand(
            Arrays.asList("javadoc"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_REBUILD_TASK = nonBlockingCommand(
            Arrays.asList("clean", "build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_TEST_SINGLE_TASK = nonBlockingCommand(
            Arrays.asList(projectTask("cleanTest"), projectTask("test")),
            Arrays.asList("-Dtest.single=${test-file-path}"),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_TASK = blockingCommand(
            Arrays.asList(projectTask("cleanTest"), projectTask("test")),
            Arrays.asList("-Dtest.single=" + StandardTaskVariable.TEST_FILE_PATH.getScriptReplaceConstant(), "-Dtest.debug"),
            Collections.<String>emptyList(),
            CustomCommandActions.DEBUG);
    private static final CommandWithActions DEFAULT_RUN_SINGLE_TASK = blockingCommand(
            Arrays.asList(projectTask("run")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            Collections.<String>emptyList(),
            CustomCommandActions.RUN);
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK = blockingCommand(
            Arrays.asList(projectTask("debug")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            Collections.<String>emptyList(),
            CustomCommandActions.DEBUG);
    private static final CommandWithActions DEFAULT_APPLY_CODE_CHANGES_TASK = blockingCommand(
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);

    private static final Map<String, CommandWithActions> DEFAULT_TASKS;
    private static final Map<String, String> DISPLAY_NAME_MAP;

    static {
        DEFAULT_TASKS = new HashMap<String, CommandWithActions>();
        addToDefaults(ActionProvider.COMMAND_BUILD, DEFAULT_BUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST, DEFAULT_TEST_TASK);
        addToDefaults(ActionProvider.COMMAND_CLEAN, DEFAULT_CLEAN_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN, DEFAULT_RUN_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG, DEFAULT_DEBUG_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_JAVADOC, DEFAULT_JAVADOC_TASK);
        addToDefaults(ActionProvider.COMMAND_REBUILD, DEFAULT_REBUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST_SINGLE, DEFAULT_TEST_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, DEFAULT_DEBUG_TEST_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN_SINGLE, DEFAULT_RUN_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG_SINGLE, DEFAULT_DEBUG_SINGLE_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_DEBUG_FIX, DEFAULT_APPLY_CODE_CHANGES_TASK);

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

    private static void addToDefaults(String command, CommandWithActions task) {
        addToMap(command, task, DEFAULT_TASKS);
    }

    private static void addToMap(String command, CommandWithActions task, Map<String, CommandWithActions> map) {
        map.put(command, task);
    }

    private static String projectTask(String task) {
        return StandardTaskVariable.PROJECT_NAME.getScriptReplaceConstant() + ":" + task;
    }

    public static String tryGetDisplayNameOfDefaultCommand(String command) {
        if (command == null) throw new NullPointerException("command");
        return DISPLAY_NAME_MAP.get(command);
    }

    public static String getDisplayNameOfCommand(NbGradleProject project, String command) {
        String displayName = tryGetDisplayNameOfDefaultCommand(command);
        if (displayName != null) {
            return displayName;
        }

        for (BuiltInGradleCommandQuery commandQuery: project.getLookup().lookupAll(BuiltInGradleCommandQuery.class)) {
            displayName = commandQuery.tryGetDisplayNameOfCommand(command);
            if (displayName != null) {
                return displayName;
            }
        }

        LOGGER.log(Level.WARNING, "Command does not have a display name: {0}", command);
        return command;
    }

    private final NbGradleProject project;
    private final Set<String> supportedCommands;

    public DefaultBuiltInTasks(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.supportedCommands = Collections.unmodifiableSet(DEFAULT_TASKS.keySet());
    }

    @Override
    public Set<String> getSupportedCommands() {
        return supportedCommands;
    }

    @Override
    public String tryGetDisplayNameOfCommand(String command) {
        return getDisplayNameOfCommand(project, command);
    }

    @Override
    public GradleCommandTemplate tryGetDefaultGradleCommand(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        return task != null ? task.command : null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        if (task != null && task.customActions.getTaskKind() == TaskKind.DEBUG) {
            return debugActions(true);
        }
        return task != null ? task.customActions : null;
    }

    private CustomCommandActions debugActions(boolean test) {
        CustomCommandActions.Builder result = new CustomCommandActions.Builder(TaskKind.DEBUG);
        result.setStdOutProcessor(new DebugTextListener(new AttacherListener(project, test)));
        return result.create();
    }

    private static CommandWithActions blockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder(taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setJvmArguments(jvmArguments);
        commandBuilder.setBlocking(true);

        return new CommandWithActions(commandBuilder.create(), customActions);
    }

    private static CommandWithActions nonBlockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder(taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setJvmArguments(jvmArguments);
        commandBuilder.setBlocking(false);

        return new CommandWithActions(commandBuilder.create(), customActions);
    }

    private static final class CommandWithActions {
        public final GradleCommandTemplate command;
        public final CustomCommandActions customActions;

        public CommandWithActions(GradleCommandTemplate command, CustomCommandActions customActions) {
            this.command = command;
            this.customActions = customActions;
        }
    }
}
