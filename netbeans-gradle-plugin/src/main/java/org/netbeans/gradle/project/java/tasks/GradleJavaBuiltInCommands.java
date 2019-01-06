package org.netbeans.gradle.project.java.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.gradle.util.GradleVersion;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.collections.CollectionsEx;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.GlobalConfig;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteListener;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.SingleExecutionOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.properties.DebugMode;
import org.netbeans.gradle.project.java.test.TestTaskName;
import org.netbeans.gradle.project.java.test.TestXmlDisplayer;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.DebugUtils;
import org.netbeans.gradle.project.tasks.vars.StandardTaskVariable;
import org.netbeans.gradle.project.view.GlobalErrorReporter;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

public final class GradleJavaBuiltInCommands implements BuiltInGradleCommandQuery {
    private static final Logger LOGGER = Logger.getLogger(GradleJavaBuiltInCommands.class.getName());

    private static final String MAIN_CLASS_PROPERTY_NAME = "mainClass";
    private static final String CMD_LINE_ARGS_PROPERTY_NAME = "cmdLineArgs";
    private static final String JVM_LINE_ARGS_PROPERTY_NAME = "jvmLineArgs";
    private static final String JPDA_PORT_PROPERTY_NAME = "debuggerJpdaPort";
    private static final String DEBUGGED_TASK_PROPERTY_NAME = "debuggedTaskName";
    private static final String DEBUG_ENABLE_ARG = "--debug-jvm";

    public static final String TEST_WITH_COVERAGE = "test/coverage";

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
    private static final CommandWithActions DEFAULT_TEST_WITH_COVERAGE_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestTasks(projectTask(JavaGradleTaskVariableQuery.COVERAGE_REPORT_TASK_NAME.getScriptReplaceConstant()))),
            Arrays.asList("-PenableJacocoXmlReport=true"),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_RUN_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList(projectTask("run")),
            Arrays.asList(cmdLineArg(), jvmLineArg()));
    private static final CommandWithActions DEFAULT_DEBUG_TASK_1 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("debug")),
            Arrays.asList(cmdLineArg(), jvmLineArg()),
            attachDebugger());
    private static final CommandWithActions DEFAULT_DEBUG_TASK_2 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("run")),
            debuggeeAttachesArguments(projectTask("run"), cmdLineArg(), jvmLineArg()),
            listenDebugger());
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
            Arrays.asList(cleanAndTestClassTasks()),
            Collections.<String>emptyList(),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_TASK_1 = blockingCommand(
            TaskKind.DEBUG,
            concat(cleanAndTestClassTasks(), DEBUG_ENABLE_ARG),
            Collections.<String>emptyList(),
            displayTestResults(),
            hideTestFailures(),
            attachDebugger());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_TASK_2 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestClassTasks()),
            debuggeeAttachesArguments(testTask()),
            displayTestResults(),
            hideTestFailures(),
            listenDebugger());
    private static final CommandWithActions DEFAULT_TEST_SINGLE_METHOD_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(cleanAndTestMethodTasks()),
            Collections.<String>emptyList(),
            needsGradle("1.10"),
            displayTestResults(),
            hideTestFailures());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK_1 = blockingCommand(
            TaskKind.DEBUG,
            concat(cleanAndTestMethodTasks(), DEBUG_ENABLE_ARG),
            Collections.<String>emptyList(),
            needsGradle("1.10"),
            displayTestResults(),
            hideTestFailures(),
            attachDebugger());
    private static final CommandWithActions DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK_2 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(cleanAndTestMethodTasks()),
            debuggeeAttachesArguments(testTask()),
            needsGradle("1.10"),
            displayTestResults(),
            hideTestFailures(),
            listenDebugger());
    private static final CommandWithActions DEFAULT_RUN_SINGLE_TASK = blockingCommand(
            TaskKind.RUN,
            Arrays.asList(projectTask("run")),
            Arrays.asList(gradlePropertyArg(MAIN_CLASS_PROPERTY_NAME, StandardTaskVariable.SELECTED_CLASS.getVariable()), cmdLineArg(), jvmLineArg()),
            true,
            true);
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK_1 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("debug")),
            Arrays.asList(gradlePropertyArg(MAIN_CLASS_PROPERTY_NAME, StandardTaskVariable.SELECTED_CLASS.getVariable()), cmdLineArg(), jvmLineArg()),
            true,
            true,
            attachDebugger());
    private static final CommandWithActions DEFAULT_DEBUG_SINGLE_TASK_2 = blockingCommand(
            TaskKind.DEBUG,
            Arrays.asList(projectTask("run")),
            debuggeeAttachesArguments(projectTask("run"), gradlePropertyArg(MAIN_CLASS_PROPERTY_NAME, StandardTaskVariable.SELECTED_CLASS.getVariable()), cmdLineArg(), jvmLineArg()),
            true,
            true,
            listenDebugger());
    private static final CommandWithActions DEFAULT_APPLY_CODE_CHANGES_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList(),
            applyClassesActions());
    private static final CommandWithActions DEFAULT_COMPILE_SINGLE_TASK = nonBlockingCommand(
            TaskKind.BUILD,
            Arrays.asList(projectTask("classes")),
            Collections.<String>emptyList());

    private static final Map<String, CommandWithActionsRef> DEFAULT_TASKS;

    static {
        DEFAULT_TASKS = new HashMap<>();
        addToDefaults(ActionProvider.COMMAND_BUILD, DEFAULT_BUILD_TASK);
        addToDefaults(ActionProvider.COMMAND_TEST, DEFAULT_TEST_TASK);
        addToDefaults(TEST_WITH_COVERAGE, DEFAULT_TEST_WITH_COVERAGE_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN, DEFAULT_RUN_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_JAVADOC, DEFAULT_JAVADOC_TASK);
        addToDefaults(ActionProvider.COMMAND_REBUILD, DEFAULT_REBUILD_TASK);
        addToDefaults(SingleMethod.COMMAND_RUN_SINGLE_METHOD, DEFAULT_TEST_SINGLE_METHOD_TASK);
        addToDefaults(ActionProvider.COMMAND_RUN_SINGLE, DEFAULT_RUN_SINGLE_TASK);
        addToDefaults(JavaProjectConstants.COMMAND_DEBUG_FIX, DEFAULT_APPLY_CODE_CHANGES_TASK);
        addToDefaults(ActionProvider.COMMAND_COMPILE_SINGLE, DEFAULT_COMPILE_SINGLE_TASK);

        addToDefaults(ActionProvider.COMMAND_TEST_SINGLE, DEFAULT_TEST_SINGLE_TASK);

        addToDefaults(ActionProvider.COMMAND_DEBUG_TEST_SINGLE, debugModeSelector(), Arrays.asList(
                new CommandChoice<>(DebugMode.DEBUGGER_ATTACHES, DEFAULT_DEBUG_TEST_SINGLE_TASK_1),
                new CommandChoice<>(DebugMode.DEBUGGER_LISTENS, DEFAULT_DEBUG_TEST_SINGLE_TASK_2)
        ));
        addToDefaults(SingleMethod.COMMAND_DEBUG_SINGLE_METHOD, debugModeSelector(), Arrays.asList(
                new CommandChoice<>(DebugMode.DEBUGGER_ATTACHES, DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK_1),
                new CommandChoice<>(DebugMode.DEBUGGER_LISTENS, DEFAULT_DEBUG_TEST_SINGLE_METHOD_TASK_2)
        ));

        addToDefaults(ActionProvider.COMMAND_DEBUG, debugModeSelector(), Arrays.asList(
                new CommandChoice<>(DebugMode.DEBUGGER_ATTACHES, DEFAULT_DEBUG_TASK_1),
                new CommandChoice<>(DebugMode.DEBUGGER_LISTENS, DEFAULT_DEBUG_TASK_2)
        ));
        addToDefaults(ActionProvider.COMMAND_DEBUG_SINGLE, debugModeSelector(), Arrays.asList(
                new CommandChoice<>(DebugMode.DEBUGGER_ATTACHES, DEFAULT_DEBUG_SINGLE_TASK_1),
                new CommandChoice<>(DebugMode.DEBUGGER_LISTENS, DEFAULT_DEBUG_SINGLE_TASK_2)
        ));
    }

    private static String cmdLineArg() {
        return gradlePropertyArg(CMD_LINE_ARGS_PROPERTY_NAME, StandardTaskVariable.CMD_LINE_ARGS.getVariable());
    }

    private static String jvmLineArg() {
        return gradlePropertyArg(JVM_LINE_ARGS_PROPERTY_NAME, StandardTaskVariable.JVM_LINE_ARGS.getVariable());
    }

    private static String gradlePropertyArg(String propertyName, TaskVariable taskVar) {
        return gradlePropertyArg(propertyName, taskVar.getScriptReplaceConstant());
    }

    private static String gradlePropertyArg(String propertyName, String value) {
        // TODO: Escape value
        return "-P" + propertyName + "=" + value;
    }

    private static CommandSelector<DebugMode> debugModeSelector() {
        return (JavaExtension javaExt) -> {
            return javaExt.getProjectProperties().debugMode().getActiveValue();
        };
    }

    private static <ChoiceType> void addToDefaults(
            String command,
            final CommandSelector<? extends ChoiceType> selector,
            Collection<CommandChoice<ChoiceType>> choices) {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(choices, "choices");

        List<CommandChoice<ChoiceType>> choicesCopy = CollectionsEx.readOnlyCopy(choices);

        DEFAULT_TASKS.put(command, (JavaExtension javaExt) -> {
            ChoiceType choice = selector.choose(javaExt);
            for (CommandChoice<ChoiceType> candidate: choicesCopy) {
                if (Objects.equals(candidate.getChoice(), choice)) {
                    return candidate.getCommandWithActions();
                }
            }
            return choicesCopy.get(0).getCommandWithActions();
        });
    }

    private static void addToDefaults(String command, CommandWithActions task) {
        DEFAULT_TASKS.put(command, javaExt -> task);
    }

    private static List<String> debuggeeAttachesArguments(String taskName, String... additionalArgs) {
        List<String> result = new ArrayList<>(additionalArgs.length + 2);
        result.addAll(Arrays.asList(additionalArgs));
        result.add(gradlePropertyArg(DEBUGGED_TASK_PROPERTY_NAME, taskName));
        result.add(gradlePropertyArg(JPDA_PORT_PROPERTY_NAME, DebuggerServiceFactory.JPDA_PORT_VAR));
        return result;
    }

    private static String cleanTestTask() {
        return projectTask("clean" + JavaGradleTaskVariableQuery.TEST_TASK_NAME_CAPITAL.getScriptReplaceConstant());
    }

    private static String testTask() {
        return projectTask(JavaGradleTaskVariableQuery.TEST_TASK_NAME.getScriptReplaceConstant());
    }

    private static List<String> concat(String[] base, String... extra) {
        return concat(Arrays.asList(base), extra);
    }

    private static List<String> concat(List<String> base, String... extra) {
        List<String> result = new ArrayList<>(base.size() + extra.length);
        result.addAll(base);
        result.addAll(Arrays.asList(extra));
        return result;
    }

    private static String[] cleanAndTestClassTasks() {
        return cleanAndTestTasks(StandardTaskVariable.TEST_CLASSES_STARED_ARGS.getScriptReplaceConstant());
    }

    private static String[] cleanAndTestMethodTasks() {
        return cleanAndTestTasks(StandardTaskVariable.TEST_METHODS_ARGS.getScriptReplaceConstant());
    }

    private static String[] cleanAndTestTasks(String... testTaskOptions) {
        String[] result = new String[testTaskOptions.length + 2];
        result[0] = cleanTestTask();
        result[1] = testTask();
        System.arraycopy(testTaskOptions, 0, result, 2, testTaskOptions.length);
        return result;
    }

    private static String projectTask(String task) {
        return StandardTaskVariable.PROJECT_PATH_NORMALIZED.getScriptReplaceConstant() + ":" + task;
    }

    private final JavaExtension javaExt;
    private final Set<String> supportedCommands;

    public GradleJavaBuiltInCommands(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.supportedCommands = Collections.unmodifiableSet(DEFAULT_TASKS.keySet());
    }

    @Override
    public Set<String> getSupportedCommands() {
        return supportedCommands;
    }

    @Override
    public String tryGetDisplayNameOfCommand(String command) {
        if (TEST_WITH_COVERAGE.equals(command)) {
            return NbStrings.getTestWithCoverageCommandCaption();
        }

        // The contract of this method allows us to rely on the defaults.
        return null;
    }

    private CommandWithActions tryGetDefaultCommandWithActions(String command) {
        CommandWithActionsRef ref = DEFAULT_TASKS.get(command);
        return ref != null ? ref.get(javaExt) : null;
    }

    @Override
    public GradleCommandTemplate tryGetDefaultGradleCommand(ProfileDef profileDef, String command) {
        CommandWithActions task = tryGetDefaultCommandWithActions(command);
        return task != null ? task.getCommand() : null;
    }

    @Override
    public CustomCommandActions tryGetCommandDefs(ProfileDef profileDef, String command) {
        CommandWithActions task = tryGetDefaultCommandWithActions(command);
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

    private static CustomCommandAdjuster hideTestFailures() {
        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setCommandExceptionHider(GradleJavaBuiltInCommands::isTestFailureException);
        };
    }

    private static String removeLeadingColons(String str) {
        if (!str.startsWith(":")) {
            return str;
        }

        int i;
        for (i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ':') {
                break;
            }
        }

        return str.substring(i);
    }

    private static String tryRelativizeTaskName(String projectPath, String taskName) {
        if (taskName.startsWith(":")) {
            if (!taskName.startsWith(projectPath)) {
                return null;
            }

            return removeLeadingColons(taskName.substring(projectPath.length()));
        }
        else {
            return taskName;
        }
    }

    private static List<String> filterTestTaskNames(JavaExtension javaExt, List<String> taskNames) {
        NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();

        List<String> result = new ArrayList<>();
        for (String taskName: taskNames) {
            String projectPath = mainModule.getProperties().getProjectFullName();
            String relativeTaskName = tryRelativizeTaskName(projectPath, taskName);

            if (relativeTaskName != null) {
                JavaTestTask testTask = mainModule.tryGetTestModelByName(relativeTaskName);
                if (testTask != null) {
                    result.add(testTask.getName());
                }
            }
        }

        return result;
    }

    private static List<String> getTestNames(JavaExtension javaExt, ExecutedCommandContext executedCommandContext) {
        List<String> requestedTaskNames = filterTestTaskNames(javaExt, executedCommandContext.getTaskNames());
        if (!requestedTaskNames.isEmpty()) {
            return requestedTaskNames;
        }

        TaskVariableMap variables = executedCommandContext.getTaskVariables();
        String value = variables.tryGetValueForVariable(JavaGradleTaskVariableQuery.TEST_TASK_NAME);
        if (value == null) {
            LOGGER.warning("Could not find test task name variable.");
            value = TestTaskName.DEFAULT_TEST_TASK_NAME;
        }
        return Collections.singletonList(value);
    }

    private static ContextAwareCommandCompleteListener displayTestResults(
            final Project project,
            final JavaExtension javaExt,
            final Lookup startContext) {
        return (ExecutedCommandContext executedCommandContext, Throwable error) -> {
            displayTestReports(project, javaExt, executedCommandContext, startContext, error);
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
            JavaExtension javaExt,
            ExecutedCommandContext executedCommandContext,
            Lookup startContext,
            Throwable error) {

        List<String> testNames = getTestNames(javaExt, executedCommandContext);

        for (String testName: testNames) {
            TestXmlDisplayer xmlDisplayer = new TestXmlDisplayer(project, testName);
            if (!xmlDisplayer.displayReport(startContext)) {
                if (error == null) {
                    displayErrorDueToNoTestReportsFound(xmlDisplayer);
                }
            }
        }
    }

    private static ContextAwareCommandCompleteAction displayTestAction(JavaExtension javaExt) {
        return (Project project, Lookup commandContext) -> displayTestResults(project, javaExt, commandContext);
    }

    private static CustomCommandAdjuster displayTestResults() {
        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setContextAwareFinalizer(displayTestAction(javaExt));
        };
    }

    private static CustomCommandAdjuster needsGradle(String minGradleVersionStr) {
        final GradleVersion minGradleVersion = GradleVersion.version(minGradleVersionStr);

        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setGradleTargetVerifier((gradleTarget, output, errOutput) -> {
                if (gradleTarget.getGradleVersion().compareTo(minGradleVersion) < 0) {
                    errOutput.println(NbStrings.getNeedsMinGradleVersion(minGradleVersion));
                    return false;
                }

                return true;
            });
        };
    }

    private static SingleExecutionOutputProcessor attachDebuggerListener(
            final JavaExtension javaExt,
            CustomCommandActions.Builder customActions) {
        final CancellationSource cancel = Cancellation.createCancellationSource();
        customActions.setCancelToken(cancel.getToken());

        return (project) -> new DebugTextListener(new AttacherListener(javaExt, cancel.getController()));
    }

    private static CustomCommandAdjuster listenDebugger() {
        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setCommandServiceFactory(new DebuggerServiceFactory(javaExt));
        };
    }

    private static CustomCommandAdjuster attachDebugger() {
        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setSingleExecutionStdOutProcessor(attachDebuggerListener(javaExt, customActions));
        };
    }

    private static ContextAwareCommandFinalizer applyClassesFinalizer(final Project project, final String className) {
        return (OutputWriter output, OutputWriter errOutput) -> {
            DebugUtils.applyChanges(project, output, className);
        };
    }

    private static CustomCommandAdjuster applyClassesActions() {
        return (JavaExtension javaExt, CustomCommandActions.Builder customActions) -> {
            customActions.setContextAwareAction((Project project, Lookup commandContext) -> {
                String className = DebugUtils.getActiveClassName(project, commandContext);
                return applyClassesFinalizer(project, className);
            });
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

    private static final class CommandChoice<ChoiceType> {
        private final ChoiceType choice;
        private final CommandWithActions commandWithActions;

        public CommandChoice(ChoiceType choice, CommandWithActions commandWithActions) {
            this.choice = Objects.requireNonNull(choice, "choice");
            this.commandWithActions = Objects.requireNonNull(commandWithActions, "commandWithActions");
        }

        public ChoiceType getChoice() {
            return choice;
        }

        public CommandWithActions getCommandWithActions() {
            return commandWithActions;
        }
    }

    private static interface CommandSelector<ChoiceType> {
        public ChoiceType choose(JavaExtension javaExt);
    }

    private static interface CommandWithActionsRef {
        public CommandWithActions get(JavaExtension javaExt);
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
            boolean skipTests = skipTestsIfNeeded && GlobalConfig.skipTests().getValue();
            boolean skipCheck = skipCheckIfNeeded && GlobalConfig.skipCheck().getValue();

            if (skipTests || skipCheck) {
                GradleCommandTemplate.Builder builder = new GradleCommandTemplate.Builder(command);
                List<String> prevArguments = command.getArguments();
                List<String> newArguments = new ArrayList<>(prevArguments.size() + 4);
                newArguments.addAll(prevArguments);
                if (skipTests) {
                    newArguments.add("-x");
                    newArguments.add(TestTaskName.DEFAULT_TEST_TASK_NAME);
                }
                if (skipCheck) {
                    newArguments.add("-x");
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
