package org.netbeans.gradle.project.java.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.GlobalConfig;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.ContextAwareCommandAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.DebugUtils;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.spi.project.ActionProvider;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

public final class GradleJavaBuiltInCommands implements BuiltInGradleCommandQuery {
    private static final CommandWithActions DEFAULT_BUILD_TASK = nonBlockingCommand(
            Arrays.asList("build"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD,
            true);
    private static final CommandWithActions DEFAULT_TEST_TASK = nonBlockingCommand(
            Arrays.asList("cleanTest", "test"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            CustomCommandActions.BUILD);
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
            CustomCommandActions.BUILD,
            true);
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
            CustomCommandActions.RUN,
            true);
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK = blockingCommand(
            Arrays.asList(projectTask("debug")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            Collections.<String>emptyList(),
            CustomCommandActions.DEBUG,
            true);
    private static final CommandWithActions DEFAULT_APPLY_CODE_CHANGES_TASK = blockingCommand(
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            applyClassesActions());

    private static final Map<String, CommandWithActions> DEFAULT_TASKS;

    static {
        DEFAULT_TASKS = new HashMap<String, CommandWithActions>();
        addToDefaults(ActionProvider.COMMAND_BUILD, DEFAULT_BUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST, DEFAULT_TEST_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG, DEFAULT_DEBUG_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_JAVADOC, DEFAULT_JAVADOC_TASK);
        addToDefaults(ActionProvider.COMMAND_REBUILD, DEFAULT_REBUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST_SINGLE, DEFAULT_TEST_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, DEFAULT_DEBUG_TEST_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN_SINGLE, DEFAULT_RUN_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG_SINGLE, DEFAULT_DEBUG_SINGLE_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_DEBUG_FIX, DEFAULT_APPLY_CODE_CHANGES_TASK);
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

    private final JavaExtension javaExt;
    private final Set<String> supportedCommands;

    public GradleJavaBuiltInCommands(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");

        this.javaExt = javaExt;
        this.supportedCommands = Collections.unmodifiableSet(DEFAULT_TASKS.keySet());
    }

    @Override
    public Set<String> getSupportedCommands() {
        return supportedCommands;
    }

    @Override
    public String tryGetDisplayNameOfCommand(String command) {
        // The contract of this method allows us to rely on the defaults.
        return null;
    }

    @Override
    public GradleCommandTemplate tryGetDefaultGradleCommand(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        return task != null ? task.getCommand() : null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        CommandWithActions task = DEFAULT_TASKS.get(command);
        if (task != null && task.getCustomActions().getTaskKind() == TaskKind.DEBUG) {
            return debugActions(true);
        }
        return task != null ? task.getCustomActions() : null;
    }

    private CustomCommandActions debugActions(boolean test) {
        CustomCommandActions.Builder result = new CustomCommandActions.Builder(TaskKind.DEBUG);
        result.setStdOutProcessor(new DebugTextListener(new AttacherListener(javaExt, test)));
        return result.create();
    }

    private static ContextAwareCommandFinalizer applyClassesFinalizer(final Project project, final String className) {
        return new ContextAwareCommandFinalizer() {
            @Override
            public void finalizeSuccessfulCommand(OutputWriter output, OutputWriter errOutput) {
                DebugUtils.applyChanges(project, output, className);
            }
        };
    }

    private static CustomCommandActions applyClassesActions() {
        CustomCommandActions.Builder result = new CustomCommandActions.Builder(TaskKind.BUILD);
        result.setContextAwareAction(new ContextAwareCommandAction() {
            @Override
            public ContextAwareCommandFinalizer startCommand(Project project, Lookup commandContext) {
                String className = DebugUtils.getActiveClassName(project, commandContext);
                return applyClassesFinalizer(project, className);
            }
        });
        return result.create();
    }

    private static CommandWithActions blockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions) {

        return blockingCommand(taskNames, arguments, jvmArguments, customActions, false);
    }

    private static CommandWithActions blockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions,
            boolean skipTestsIfNeeded) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder(taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setJvmArguments(jvmArguments);
        commandBuilder.setBlocking(true);

        return new CommandWithActions(commandBuilder.create(), customActions, skipTestsIfNeeded);
    }

    private static CommandWithActions nonBlockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions) {
        return nonBlockingCommand(taskNames, arguments, jvmArguments, customActions, false);
    }

    private static CommandWithActions nonBlockingCommand(
            List<String> taskNames,
            List<String> arguments,
            List<String> jvmArguments,
            CustomCommandActions customActions,
            boolean skipTestsIfNeeded) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder(taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setJvmArguments(jvmArguments);
        commandBuilder.setBlocking(false);

        return new CommandWithActions(commandBuilder.create(), customActions, skipTestsIfNeeded);
    }

    private static final class CommandWithActions {
        private final GradleCommandTemplate command;
        private final CustomCommandActions customActions;
        private final boolean skipTestsIfNeeded;

        public CommandWithActions(
                GradleCommandTemplate command,
                CustomCommandActions customActions,
                boolean skipTestIfNeeded) {
            this.command = command;
            this.customActions = customActions;
            this.skipTestsIfNeeded = skipTestIfNeeded;
        }

        public GradleCommandTemplate getCommand() {
            if (skipTestsIfNeeded && GlobalConfig.skipTests().getValue()) {
                GradleCommandTemplate.Builder builder = new GradleCommandTemplate.Builder(command);
                List<String> prevArguments = command.getArguments();
                List<String> newArguments = new ArrayList<String>(prevArguments.size() + 2);
                newArguments.add("-x");
                newArguments.add("test");
                builder.setArguments(newArguments);
                return builder.create();
            }
            return command;
        }

        public CustomCommandActions getCustomActions() {
            return customActions;
        }
    }
}
