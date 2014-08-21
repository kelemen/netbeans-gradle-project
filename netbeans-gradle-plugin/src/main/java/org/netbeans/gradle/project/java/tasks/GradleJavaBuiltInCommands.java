package org.netbeans.gradle.project.java.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.gradle.util.GradleVersion;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.GlobalConfig;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.CommandExceptionHider;
import org.netbeans.gradle.project.api.task.ContextAwareCommandAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteListener;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.GradleTargetVerifier;
import org.netbeans.gradle.project.api.task.SingleExecutionOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.api.task.TaskOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.gradle.project.java.test.TestXmlDisplayer;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.DebugUtils;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
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
            true,
            true);
    private static final CommandWithActions DEFAULT_TEST_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks()),
            Collections.<String>emptyList(),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_RUN_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList("run"),
            Collections.<String>emptyList());
    private static final CommandWithActions DEFAULT_DEBUG_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList("debug"),
            Collections.<String>emptyList(),
            attachDebugger());
    private static final CommandWithActions DEFAULT_JAVADOC_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList("javadoc"),
            Collections.<String>emptyList());
    private static final CommandWithActions DEFAULT_REBUILD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList("clean", "build"),
            Collections.<String>emptyList(),
            true,
            true);
    private static final CommandWithActions DEFAULT_TEST_SINGLE_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList(testSingleArgument()),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestTasks()),
            Arrays.asList(testSingleArgument(), debugTestArgument()),
            displayTestResults(),
            hideTestFailures(),
            attachDebugger());
    private static final CommandWithActions DEFAULT_TEST_SINGLE_METHOD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestMethodTasks()),
            Collections.<String>emptyList(),
            needsGradle("1.10"),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestMethodTasks()),
            Arrays.asList(debugTestArgument()),
            needsGradle("1.10"),
            displayTestResults(),
            hideTestFailures(),
            attachDebugger());
    private static final CommandWithActions DEFAULT_RUN_SINGLE_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList(projectTask("run")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            true,
            true);
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("debug")),
            Arrays.asList("-PmainClass=" + StandardTaskVariable.SELECTED_CLASS.getScriptReplaceConstant()),
            true,
            true,
            attachDebugger());
    private static final CommandWithActions DEFAULT_APPLY_CODE_CHANGES_TASK = blockingCommand(
            TaskKind.BUILD,
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList(),
            applyClassesActions());

    private static final Map<String, CommandWithActions> DEFAULT_TASKS;

    static {
        DEFAULT_TASKS = new HashMap<>();
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

    private static String[] cleanAndTestMethodTasks() {
        return cleanAndTestTasks("--tests", StandardTaskVariable.TEST_METHOD.getScriptReplaceConstant());
    }

    private static String[] cleanAndTestTasks(String... testTaskOptions) {
        String[] result = new String[testTaskOptions.length + 2];
        result[0] = cleanTestTask();
        result[1] = testTask();
        System.arraycopy(testTaskOptions, 0, result, 2, testTaskOptions.length);
        return result;
    }

    private static String projectTask(String task) {
        return StandardTaskVariable.PROJECT_NAME.getScriptReplaceConstant() + ":" + task;
    }

    private final JavaExtension javaExt;
    private final Set<String> supportedCommands;

    public GradleJavaBuiltInCommands(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

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
        return task != null ? task.getCustomActions(javaExt) : null;
    }

    private static boolean isTestFailureException(Throwable taskError) {
        if (!(taskError instanceof Exception)) {
            return false;
        }

        Throwable rootCause = Exceptions.getRootCause(taskError);

        String message = rootCause.getMessage();
        if (message == null) {
            return false;
        }

        message = message.toLowerCase(Locale.ROOT);
        return message.contains("there were failing tests");
    }

    private static CommandExceptionHider testFailureHider() {
        return new CommandExceptionHider() {
            @Override
            public boolean hideException(Throwable taskError) {
                return isTestFailureException(taskError);
            }
        };
    }

    private static CustomCommandAdjuster hideTestFailures() {
        return new CustomCommandAdjuster() {
            @Override
            public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions) {
                customActions.setCommandExceptionHider(testFailureHider());
            }
        };
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

    private static ContextAwareCommandCompleteListener displayTestResults(
            final Project project,
            final Lookup startContext) {
        return new ContextAwareCommandCompleteListener() {
            @Override
            public void onComplete(ExecutedCommandContext executedCommandContext, Throwable error) {
                displayTestReports(project, executedCommandContext, startContext, error);
            }
        };
    }

    private static void displayErrorDueToNoTestReportsFound(TestXmlDisplayer xmlDisplayer) {
        String message = NbStrings.getErrorDueToNoTestReportsFound(
                xmlDisplayer.getTestName(),
                xmlDisplayer.tryGetReportDirectory());

        GlobalErrorReporter.showIssue(message);
    }

    private static void displayTestReports(
            Project project,
            ExecutedCommandContext executedCommandContext,
            Lookup startContext,
            Throwable error) {

        String testName = getTestName(executedCommandContext);

        TestXmlDisplayer xmlDisplayer = new TestXmlDisplayer(project, testName);
        if (!xmlDisplayer.displayReport(startContext)) {
            if (error == null) {
                displayErrorDueToNoTestReportsFound(xmlDisplayer);
            }
        }
    }

    private static ContextAwareCommandCompleteAction displayTestAction() {
        return new ContextAwareCommandCompleteAction() {
            @Override
            public ContextAwareCommandCompleteListener startCommand(Project project, Lookup commandContext) {
                return displayTestResults(project, commandContext);
            }
        };
    }

    private static CustomCommandAdjuster displayTestResults() {
        return new CustomCommandAdjuster() {
            @Override
            public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions) {
                customActions.setContextAwareFinalizer(displayTestAction());
            }
        };
    }

    private static CustomCommandAdjuster needsGradle(String minGradleVersionStr) {
        final GradleVersion minGradleVersion = GradleVersion.version(minGradleVersionStr);

        return new CustomCommandAdjuster() {
            @Override
            public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions) {
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

    private static SingleExecutionOutputProcessor attachDebuggerListener(
            final JavaExtension javaExt,
            CustomCommandActions.Builder customActions) {
        final CancellationSource cancel = Cancellation.createCancellationSource();
        customActions.setCancelToken(cancel.getToken());

        return new SingleExecutionOutputProcessor() {
            @Override
            public TaskOutputProcessor startExecution(Project project) {
                return new DebugTextListener(new AttacherListener(javaExt, cancel.getController()));
            }
        };
    }

    private static CustomCommandAdjuster attachDebugger() {
        return new CustomCommandAdjuster() {
            @Override
            public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions) {
                customActions.setSingleExecutionStdOutProcessor(attachDebuggerListener(javaExt, customActions));
            }
        };
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
            public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions) {
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

        return blockingCommand(taskKind, taskNames, arguments, false, false, adjusters);
    }

    private static CommandWithActions blockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            boolean skipTestsIfNeeded,
            boolean skipCheckIfNeeded,
            CustomCommandAdjuster... adjusters) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder("", taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setBlocking(true);

        return new CommandWithActions(taskKind, commandBuilder.create(), adjusters, skipTestsIfNeeded, skipCheckIfNeeded);
    }

    private static CommandWithActions nonBlockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            CustomCommandAdjuster... adjusters) {
        return nonBlockingCommand(taskKind, taskNames, arguments, false, false, adjusters);
    }

    private static CommandWithActions nonBlockingCommand(
            TaskKind taskKind,
            List<String> taskNames,
            List<String> arguments,
            boolean skipTestsIfNeeded,
            boolean skipCheckIfNeeded,
            CustomCommandAdjuster... adjusters) {

        GradleCommandTemplate.Builder commandBuilder = new GradleCommandTemplate.Builder("", taskNames);
        commandBuilder.setArguments(arguments);
        commandBuilder.setBlocking(false);

        return new CommandWithActions(taskKind, commandBuilder.create(), adjusters, skipTestsIfNeeded, skipCheckIfNeeded);
    }

    private static CustomCommandActions createCustomActions(
            TaskKind taskKind, JavaExtension javaExt, CustomCommandAdjuster... adjusters) {
        if (adjusters.length == 0) {
            return CustomCommandActions.simpleAction(taskKind);
        }

        CustomCommandActions.Builder result = new CustomCommandActions.Builder(taskKind);
        for (CustomCommandAdjuster adjuster: adjusters) {
            adjuster.adjust(javaExt, result);
        }
        return result.create();
    }

    private static final class CommandWithActions {
        private final TaskKind taskKind;
        private final GradleCommandTemplate command;
        private final CustomCommandAdjuster[] customActions;
        private final boolean skipTestsIfNeeded;
        private final boolean skipCheckIfNeeded;

        public CommandWithActions(
                TaskKind taskKind,
                GradleCommandTemplate command,
                CustomCommandAdjuster[] customActions,
                boolean skipTestIfNeeded,
                boolean skipCheckIfNeeded) {
            this.taskKind = taskKind;
            this.command = command;
            this.customActions = customActions.clone();
            this.skipTestsIfNeeded = skipTestIfNeeded;
            this.skipCheckIfNeeded = skipCheckIfNeeded;
        }

        public GradleCommandTemplate getCommand() {
            if (skipTestsIfNeeded && GlobalConfig.skipTests().getValue()
                    || skipCheckIfNeeded && GlobalConfig.skipCheck().getValue()) {
                GradleCommandTemplate.Builder builder = new GradleCommandTemplate.Builder(command);
                List<String> prevArguments = command.getArguments();
                List<String> newArguments = new ArrayList<>(prevArguments.size() + 2);
                newArguments.addAll(prevArguments);
                newArguments.add("-x");
                if (skipTestsIfNeeded) {
                    newArguments.add(TestTaskName.DEFAULT_TEST_TASK_NAME);
                }
                if (skipCheckIfNeeded) {
                    newArguments.add("check");
                }
                builder.setArguments(newArguments);
                return builder.create();
            }
            return command;
        }

        public CustomCommandActions getCustomActions(JavaExtension javaExt) {
            return createCustomActions(taskKind, javaExt, customActions);
        }
    }

    private interface CustomCommandAdjuster {
        public void adjust(JavaExtension javaExt, CustomCommandActions.Builder customActions);
    }
}
