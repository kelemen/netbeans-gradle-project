package org.netbeans.gradle.project.tasks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;

public final class DefaultBuiltInTasks implements BuiltInGradleCommandQuery {
    private static final Logger LOGGER = Logger.getLogger(DefaultBuiltInTasks.class.getName());

    private static final CommandWithActions DEFAULT_BUILD_TASK = nonBlockingCommand(
            Arrays.asList("build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_CLEAN_TASK = nonBlockingCommand(
            Arrays.asList("clean"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_REBUILD_TASK = nonBlockingCommand(
            Arrays.asList("clean", "build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
    private static final CommandWithActions DEFAULT_TEST_TASK = nonBlockingCommand(
            Arrays.asList(TestTaskName.DEFAULT_CLEAN_TEST_TASK_NAME, TestTaskName.DEFAULT_TEST_TASK_NAME),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);

    private static final Map<String, CommandWithActions> DEFAULT_TASKS;
    private static final Map<String, String> DISPLAY_NAME_MAP;

    static {
        DEFAULT_TASKS = new HashMap<>();
        addToDefaults(ActionProvider.COMMAND_BUILD, DEFAULT_BUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_CLEAN, DEFAULT_CLEAN_TASK);
        addToDefaults(ActionProvider.COMMAND_REBUILD, DEFAULT_REBUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST, DEFAULT_TEST_TASK);

        // We have to provide names for the following commands because of the
        // contract of BuiltInGradleCommandQuery.tryGetDisplayNameOfCommand.
        DISPLAY_NAME_MAP = new HashMap<>();
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_BUILD, NbStrings.getBuildCommandCaption(false));
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST, NbStrings.getTestCommandCaption(false));
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_CLEAN, NbStrings.getCleanCommandCaption(false));
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_RUN, NbStrings.getRunCommandCaption(false));
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG, NbStrings.getDebugCommandCaption(false));
        DISPLAY_NAME_MAP.put(JavaProjectConstants.COMMAND_JAVADOC, NbStrings.getJavadocCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_REBUILD, NbStrings.getRebuildCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_TEST_SINGLE, NbStrings.getTestSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, NbStrings.getDebugTestSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_RUN_SINGLE, NbStrings.getRunSingleCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_DEBUG_SINGLE, NbStrings.getDebugSingleCommandCaption());
        DISPLAY_NAME_MAP.put(JavaProjectConstants.COMMAND_DEBUG_FIX, NbStrings.getApplyCodeChangesCommandCaption());
        DISPLAY_NAME_MAP.put(SingleMethod.COMMAND_RUN_SINGLE_METHOD, NbStrings.getTestSingleMethodCommandCaption());
        DISPLAY_NAME_MAP.put(SingleMethod.COMMAND_DEBUG_SINGLE_METHOD, NbStrings.getDebugTestSingleMethodCommandCaption());
        DISPLAY_NAME_MAP.put(ActionProvider.COMMAND_COMPILE_SINGLE, NbStrings.getCompileSingleCaption());
    }

    private static void addToDefaults(String command, CommandWithActions task) {
        addToMap(command, task, DEFAULT_TASKS);
    }

    private static void addToMap(String command, CommandWithActions task, Map<String, CommandWithActions> map) {
        map.put(command, task);
    }

    private static String tryGetDisplayNameOfDefaultCommand(String command) {
        Objects.requireNonNull(command, "command");
        return DISPLAY_NAME_MAP.get(command);
    }

    public static String getDisplayNameOfCommand(NbGradleProject project, String command) {
        String displayName = tryGetDisplayNameOfDefaultCommand(command);
        if (displayName != null) {
            return displayName;
        }

        for (BuiltInGradleCommandQuery commandQuery: project.getExtensions().lookupAllExtensionObjs(BuiltInGradleCommandQuery.class)) {
            displayName = commandQuery.tryGetDisplayNameOfCommand(command);
            if (displayName != null) {
                return displayName;
            }
        }

        LOGGER.log(Level.WARNING, "Command does not have a display name: {0}", command);
        return command;
    }

    private final Set<String> supportedCommands;

    public DefaultBuiltInTasks() {
        this.supportedCommands = Collections.unmodifiableSet(DEFAULT_TASKS.keySet());
    }

    @Override
    public Set<String> getSupportedCommands() {
        return supportedCommands;
    }

    @Override
    public String tryGetDisplayNameOfCommand(String command) {
        return DISPLAY_NAME_MAP.get(command);
    }

    @Override
    public GradleCommandTemplate tryGetDefaultGradleCommand(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        return task != null ? task.command : null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        return task != null ? task.customActions : null;
    }

    private static CommandWithActions nonBlockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder("", taskNames);
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
