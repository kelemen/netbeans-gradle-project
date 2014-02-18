package org.netbeans.gradle.project.java.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.gradle.util.GradleVersion;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.GlobalConfig;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.ContextAwareCommandAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteListener;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.GradleTargetVerifier;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.DebugUtils;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.gradle.project.tasks.TestTaskName;
import org.netbeans.gradle.project.tasks.TestXmlDisplayer;
import org.netbeans.gradle.project.view.GlobalErrorReporter;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

public final class GradleJavaBuiltInCommands implements BuiltInGradleCommandQuery {
    private static final Logger LOGGER = Logger.getLogger(GradleJavaBuiltInCommands.class.getName());

    private static final CommandWithActions DEFAULT_BUILD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList("build"),
            Collections.<String>emptyList(),
            true);
    private static final CommandWithActions DEFAULT_TEST_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks()),
            Collections.<String>emptyList(),
            displayTestResults());
    private static final CommandWithActions DEFAULT_RUN_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList("run"),
            Collections.<String>emptyList());
    private static final CommandWithActions DEFAULT_DEBUG_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList("debug"),
            Collections.<String>emptyList());
    private static final CommandWithActions DEFAULT_JAVADOC_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList("javadoc"),
            Collections.<String>emptyList());
    private static final CommandWithActions DEFAULT_REBUILD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList("clean", "build"),
            Collections.<String>emptyList(),
            true);
    private static final CommandWithActions DEFAULT_TEST_SINGLE_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList(testSingleArgument()),
            displayTestResults());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList(testSingleArgument(), debugTestArgument()),
            displayTestResults());
    private static final CommandWithActions DEFAULT_TEST_SINGLE_METHOD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList("--tests", StandardTaskVariable.TEST_METHOD.getScriptReplaceConstant()),
            needsGradle("1.10"),
            displayTestResults());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList("--tests", StandardTaskVariable.TEST_METHOD.getScriptReplaceConstant(), debugTestArgument()),
            needsGradle("1.10"),
            displayTestResults());
    private static final CommandWithActions DEFAULT_RUN_SINGLE_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList(projectTask("run")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            true);
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("debug")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            true);
    private static final CommandWithActions DEFAULT_APPLY_CODE_CHANGES_TASK = blockingCommand(
            TaskKind.BUILD,
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList(),
            applyClassesActions());

    private static final Map<String, CommandWithActions> DEFAULT_TASKS;

    static {
        DEFAULT_TASKS = new HashMap<String, CommandWithActions>();
        addToDefaults(ActionProvider.COMMAND_BUILD, DEFAULT_BUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST, DEFAULT_TEST_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN, DEFAULT_RUN_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG, DEFAULT_DEBUG_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_JAVADOC, DEFAULT_JAVADOC_TASK);
        addToDefaults(ActionProvider.COMMAND_REBUILD, DEFAULT_REBUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST_SINGLE, DEFAULT_TEST_SINGLE_TASK);
        addToDefaults(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, DEFAULT_DEBUG_TEST_SINGLE_TASK);
        addToDefaults(SingleMethod.COMMAND_RUN_SINGLE_METHOD, DEFAULT_TEST_SINGLE_METHOD_TASK);
        addToDefaults(SingleMethod.COMMAND_DEBUG_SINGLE_METHOD, DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK);
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

    private static String debugTestArgument() {
        return "-D" + JavaGradleTaskVariableQuery.TEST_TASK_NAME.getScriptReplaceConstant() + ".debug";
    }

    private static String testSingleArgument() {
        return "-D" + JavaGradleTaskVariableQuery.TEST_TASK_NAME.getScriptReplaceConstant()
                + ".single=" + StandardTaskVariable.TEST_FILE_PATH.getScriptReplaceConstant();
    }

    private static String cleanTestTask() {
        return projectTask("clean" + JavaGradleTaskVariableQuery.TEST_TASK_NAME_CAPITAL.getScriptReplaceConstant());
    }

    private static String testTask() {
        return projectTask(JavaGradleTaskVariableQuery.TEST_TASK_NAME.getScriptReplaceConstant());
    }

    private static String[] cleanAndTestTasks() {
        return new String[]{cleanTestTask(), testTask()};
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
            return debugActions(task.getCustomActions().getGradleTargetVerifier());
        }
        return task != null ? task.getCustomActions() : null;
    }

    private static String getTestName(ExecutedCommandContext executedCommandContext) {
        TaskVariableMap variables = executedCommandContext.getTaskVariables();
        String value = variables.tryGetValueForVariable(JavaGradleTaskVariableQuery.TEST_TASK_NAME);
        if (value == null) {
            LOGGER.warning("Could not find test task name variable.");
            value = TestTaskName.DEFAULT_TEST_TASK_NAME;
        }
        return value;
    }

    private static ContextAwareCommandCompleteListener displayTestResults(final Project project) {
        return new ContextAwareCommandCompleteListener() {
            @Override
            public void onComplete(ExecutedCommandContext executedCommandContext, Throwable error) {
                displayTestReports(project, executedCommandContext, error);
            }
        };
    }

    private static void displayErrorDueToNoTestReportsFound(TestXmlDisplayer xmlDisplayer) {
        String message = NbStrings.getErrorDueToNoTestReportsFound(
                xmlDisplayer.getTestName(),
                xmlDisplayer.tryGetReportDirectory());

        GlobalErrorReporter.showIssue(message, null);
    }

    private static void displayTestReports(
            Project project,
            ExecutedCommandContext executedCommandContext,
            Throwable error) {

        String testName = getTestName(executedCommandContext);

        TestXmlDisplayer xmlDisplayer = new TestXmlDisplayer(project, testName);
        if (!xmlDisplayer.displayReport()) {
            if (error == null) {
                displayErrorDueToNoTestReportsFound(xmlDisplayer);
            }
        }
    }

    private static ContextAwareCommandCompleteAction displayTestAction() {
        return new ContextAwareCommandCompleteAction() {
            @Override
            public ContextAwareCommandCompleteListener startCommand(Project project, Lookup commandContext) {
                return displayTestResults(project);
            }
        };
    }

    private static CustomCommandAdjuster displayTestResults() {
        return new CustomCommandAdjuster() {
            @Override
            public void adjust(CustomCommandActions.Builder customActions) {
                customActions.setContextAwareFinalizer(displayTestAction());
            }
        };
    }

    private static CustomCommandAdjuster needsGradle(String minGradleVersionStr) {
        final GradleVersion minGradleVersion = GradleVersion.version(minGradleVersionStr);

        return new CustomCommandAdjuster() {
            @Override
            public void adjust(CustomCommandActions.Builder customActions) {
                customActions.setGradleTargetVerifier(new GradleTargetVerifier() {
                    @Override
                    public boolean checkTaskExecutable(
                            GradleTarget gradleTarget,
                            OutputWriter output,
                            OutputWriter errOutput) {
                        if (gradleTarget.getGradleVersion().compareTo(minGradleVersion) < 0) {
                            errOutput.println(NbStrings.getNeedsMinGradleVersion(minGradleVersion));
                            return false;
                        }

                        return true;
                    }
                });
            }
        };
    }

    private CustomCommandActions debugActions(GradleTargetVerifier targetVerifier) {
        CustomCommandActions.Builder result = new CustomCommandActions.Builder(TaskKind.DEBUG);
        result.setStdOutProcessor(new DebugTextListener(new AttacherListener(javaExt)));
        result.setGradleTargetVerifier(targetVerifier);
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

    private static CustomCommandAdjuster applyClassesActions() {
        return new CustomCommandAdjuster() {
            @Override
            public void adjust(CustomCommandActions.Builder customActions) {
                customActions.setContextAwareAction(new ContextAwareCommandAction() {
                    @Override
                    public ContextAwareCommandFinalizer startCommand(Project project, Lookup commandContext) {
                        String className = DebugUtils.getActiveClassName(project, commandContext);
                        return applyClassesFinalizer(project, className);
                    }
                });
            }
        };
    }

    private static CommandWithActions blockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            CustomCommandAdjuster... adjusters) {

        return blockingCommand(taskKind, taskNames, arguments, false, adjusters);
    }

    private static CommandWithActions blockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            boolean skipTestsIfNeeded,
            CustomCommandAdjuster... adjusters) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder("", taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setBlocking(true);

        CustomCommandActions customActions = createCustomActions(taskKind, adjusters);
        return new CommandWithActions(commandBuilder.create(), customActions, skipTestsIfNeeded);
    }

    private static CommandWithActions nonBlockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            CustomCommandAdjuster... adjusters) {
        return nonBlockingCommand(taskKind, taskNames, arguments, false, adjusters);
    }

    private static CommandWithActions nonBlockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            boolean skipTestsIfNeeded,
            CustomCommandAdjuster... adjusters) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder("", taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setBlocking(false);

        CustomCommandActions customActions = createCustomActions(taskKind, adjusters);
        return new CommandWithActions(commandBuilder.create(), customActions, skipTestsIfNeeded);
    }

    private static CustomCommandActions createCustomActions(
            TaskKind taskKind, CustomCommandAdjuster... adjusters) {
        if (adjusters.length == 0) {
            return CustomCommandActions.simpleAction(taskKind);
        }

        CustomCommandActions.Builder result = new CustomCommandActions.Builder(taskKind);
        for (CustomCommandAdjuster adjuster: adjusters) {
            adjuster.adjust(result);
        }
        return result.create();
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
                newArguments.addAll(prevArguments);
                newArguments.add("-x");
                newArguments.add(TestTaskName.DEFAULT_TEST_TASK_NAME);
                builder.setArguments(newArguments);
                return builder.create();
            }
            return command;
        }

        public CustomCommandActions getCustomActions() {
            return customActions;
        }
    }

    private interface CustomCommandAdjuster {
        public void adjust(CustomCommandActions.Builder customActions);
    }
}
