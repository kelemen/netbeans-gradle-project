package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.api.task.CommandExceptionHider;
import org.netbeans.gradle.project.api.task.ContextAwareCommandAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandArguments;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteListener;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.ContextAwareGradleTargetVerifier;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.GradleTargetVerifier;
import org.netbeans.gradle.project.api.task.SingleExecutionOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.api.task.TaskOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

public final class GradleTaskDef {
    private static final Logger LOGGER = Logger.getLogger(GradleTaskDef.class.getName());

    public static final class Builder {
        private String commandName;
        private TaskOutputDef outputDef;
        private List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArguments;

        private SingleExecutionOutputProcessor stdOutListener;
        private SingleExecutionOutputProcessor stdErrListener;
        private ContextAwareCommandFinalizer successfulCommandFinalizer;
        private ContextAwareCommandCompleteListener commandFinalizer;
        private GradleTargetVerifier gradleTargetVerifier;
        private TaskVariableMap nonUserTaskVariables;
        private CommandExceptionHider commandExceptionHider;

        private boolean cleanOutput;
        private boolean nonBlocking;

        public Builder(GradleTaskDef taskDef) {
            this.commandName = taskDef.getCommandName();
            this.outputDef = taskDef.getOutputDef();
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
            this.stdOutListener = taskDef.getStdOutListener();
            this.stdErrListener = taskDef.getStdErrListener();
            this.nonBlocking = taskDef.isNonBlocking();
            this.cleanOutput = taskDef.isCleanOutput();
            this.successfulCommandFinalizer = taskDef.getSuccessfulCommandFinalizer();
            this.commandFinalizer = taskDef.getCommandFinalizer();
            this.gradleTargetVerifier = taskDef.getGradleTargetVerifier();
            this.nonUserTaskVariables = taskDef.getNonUserTaskVariables();
            this.commandExceptionHider = taskDef.getCommandExceptionHider();
        }

        public Builder(TaskOutputDef outputDef, String taskName) {
            this(outputDef, Collections.singletonList(taskName));
        }

        public Builder(TaskOutputDef outputDef, String[] taskNames) {
            this(outputDef, Arrays.asList(taskNames));
        }

        public Builder(TaskOutputDef outputDef, List<String> taskNames) {
            ExceptionHelper.checkNotNullArgument(outputDef, "outputDef");

            this.commandName = "";
            this.outputDef = outputDef;
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();
            this.stdOutListener = NoOpSingleExecutionOutputProcessor.INSTANCE;
            this.stdErrListener = NoOpSingleExecutionOutputProcessor.INSTANCE;
            this.nonBlocking = false;
            this.cleanOutput = false;
            this.successfulCommandFinalizer = NoOpSuccessfulFinalizer.INSTANCE;
            this.commandFinalizer = NoOpFinalizer.INSTANCE;
            this.gradleTargetVerifier = null;
            this.nonUserTaskVariables = EmptyTaskVarMap.INSTANCE;
            this.commandExceptionHider = NoOpExceptionHider.INSTANCE;

            if (this.taskNames.isEmpty()) {
                throw new IllegalArgumentException("At least one task is required.");
            }
        }

        public TaskVariableMap getNonUserTaskVariables() {
            return nonUserTaskVariables;
        }

        public void addNonUserTaskVariables(final TaskVariableMap nonUserTaskVariables) {
            ExceptionHelper.checkNotNullArgument(nonUserTaskVariables, "nonUserTaskVariables");

            final TaskVariableMap currentTaskVariables = this.nonUserTaskVariables;
            if (currentTaskVariables == EmptyTaskVarMap.INSTANCE) {
                setNonUserTaskVariables(nonUserTaskVariables);
            }
            else {
                this.nonUserTaskVariables = new CombinedTaskVariableMap(
                        nonUserTaskVariables, currentTaskVariables);
            }
        }

        public CommandExceptionHider getCommandExceptionHider() {
            return commandExceptionHider;
        }

        public void setCommandExceptionHider(CommandExceptionHider commandExceptionHider) {
            ExceptionHelper.checkNotNullArgument(commandExceptionHider, "commandExceptionHider");
            this.commandExceptionHider = commandExceptionHider;
        }

        public void setNonUserTaskVariables(TaskVariableMap nonUserTaskVariables) {
            ExceptionHelper.checkNotNullArgument(nonUserTaskVariables, "nonUserTaskVariables");
            this.nonUserTaskVariables = nonUserTaskVariables;
        }

        public boolean isCleanOutput() {
            return cleanOutput;
        }

        public void setCleanOutput(boolean cleanOutput) {
            this.cleanOutput = cleanOutput;
        }

        public String getCommandName() {
            return commandName;
        }

        public void setCommandName(String commandName) {
            ExceptionHelper.checkNotNullArgument(commandName, "commandName");
            this.commandName = commandName;
        }

        public TaskOutputDef getOutputDef() {
            return outputDef;
        }

        public void setOutputDef(TaskOutputDef outputDef) {
            ExceptionHelper.checkNotNullArgument(outputDef, "outputDef");
            this.outputDef = outputDef;
        }

        public boolean isNonBlocking() {
            return nonBlocking;
        }

        public void setNonBlocking(boolean nonBlocking) {
            this.nonBlocking = nonBlocking;
        }

        public List<String> getTaskNames() {
            return taskNames;
        }

        public void setTaskNames(List<String> taskNames) {
            List<String> safeTaskNames = CollectionUtils.copyNullSafeList(taskNames);
            if (safeTaskNames.isEmpty()) {
                throw new IllegalArgumentException("Must contain at least a single task.");
            }
            this.taskNames = safeTaskNames;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = CollectionUtils.copyNullSafeList(arguments);
        }

        private static <T> List<T> concatNullSafeLists(
                List<? extends T> list1,
                List<? extends T> list2) {
            List<T> result = new ArrayList<>(list1.size() + list2.size());
            result.addAll(list1);
            result.addAll(list2);

            ExceptionHelper.checkNotNullElements(result, "result");
            return result;
        }

        public void addArguments(List<String> toAdd) {
            this.arguments = Collections.unmodifiableList(concatNullSafeLists(this.arguments, toAdd));
        }

        public List<String> getJvmArguments() {
            return jvmArguments;
        }

        public void setJvmArguments(List<String> jvmArguments) {
            this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        }

        public void addJvmArguments(List<String> toAdd) {
            this.jvmArguments = Collections.unmodifiableList(concatNullSafeLists(this.jvmArguments, toAdd));
        }

        public SingleExecutionOutputProcessor getStdOutListener() {
            return stdOutListener;
        }

        public void setStdOutListener(SingleExecutionOutputProcessor stdOutListener) {
            ExceptionHelper.checkNotNullArgument(stdOutListener, "stdOutListener");
            this.stdOutListener = stdOutListener;
        }

        public SingleExecutionOutputProcessor getStdErrListener() {
            return stdErrListener;
        }

        public void setStdErrListener(SingleExecutionOutputProcessor stdErrListener) {
            ExceptionHelper.checkNotNullArgument(stdErrListener, "stdErrListener");
            this.stdErrListener = stdErrListener;
        }

        public ContextAwareCommandFinalizer getSuccessfulCommandFinalizer() {
            return successfulCommandFinalizer;
        }

        public void setSuccessfulCommandFinalizer(ContextAwareCommandFinalizer successfulCommandFinalizer) {
            ExceptionHelper.checkNotNullArgument(successfulCommandFinalizer, "successfulCommandFinalizer");
            this.successfulCommandFinalizer = successfulCommandFinalizer;
        }

        public ContextAwareCommandCompleteListener getCommandFinalizer() {
            return commandFinalizer;
        }

        public void setCommandFinalizer(ContextAwareCommandCompleteListener commandFinalizer) {
            ExceptionHelper.checkNotNullArgument(commandFinalizer, "commandFinalizer");
            this.commandFinalizer = commandFinalizer;
        }

        public GradleTargetVerifier getGradleTargetVerifier() {
            return gradleTargetVerifier;
        }

        public void setGradleTargetVerifier(GradleTargetVerifier gradleTargetVerifier) {
            this.gradleTargetVerifier = gradleTargetVerifier;
        }

        public GradleTaskDef create() {
            return new GradleTaskDef(this);
        }
    }

    private final String commandName;
    private final TaskOutputDef outputDef;
    private final List<String> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArguments;
    private final SingleExecutionOutputProcessor stdOutListener;
    private final SingleExecutionOutputProcessor stdErrListener;
    private final ContextAwareCommandFinalizer successfulCommandFinalizer;
    private final ContextAwareCommandCompleteListener commandFinalizer;
    private final GradleTargetVerifier gradleTargetVerifier;
    private final TaskVariableMap nonUserTaskVariables;
    private final CommandExceptionHider commandExceptionHider;
    private final boolean nonBlocking;
    private final boolean cleanOutput;

    private GradleTaskDef(Builder builder) {
        this.commandName = builder.getCommandName();
        this.outputDef = builder.getOutputDef();
        this.taskNames = builder.getTaskNames();
        this.arguments = builder.getArguments();
        this.jvmArguments = builder.getJvmArguments();
        this.stdOutListener = builder.getStdOutListener();
        this.stdErrListener = builder.getStdErrListener();
        this.nonBlocking = builder.isNonBlocking();
        this.cleanOutput = builder.isCleanOutput();
        this.successfulCommandFinalizer = builder.getSuccessfulCommandFinalizer();
        this.commandFinalizer = builder.getCommandFinalizer();
        this.gradleTargetVerifier = builder.getGradleTargetVerifier();
        this.nonUserTaskVariables = builder.getNonUserTaskVariables();
        this.commandExceptionHider = builder.getCommandExceptionHider();
    }

    private static String[] stringListToArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public CommandExceptionHider getCommandExceptionHider() {
        return commandExceptionHider;
    }

    public TaskVariableMap getNonUserTaskVariables() {
        return nonUserTaskVariables;
    }

    public GradleTargetVerifier getGradleTargetVerifier() {
        return gradleTargetVerifier;
    }

    public boolean isCleanOutput() {
        return cleanOutput;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getSafeCommandName() {
        return commandName.isEmpty() ? taskNames.toString() : commandName;
    }

    public TaskOutputDef getOutputDef() {
        return outputDef;
    }

    public boolean isNonBlocking() {
        return nonBlocking;
    }

    public List<String> getTaskNames() {
        return taskNames;
    }

    public String[] getTaskNamesArray() {
        return stringListToArray(taskNames);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String[] getArgumentArray() {
        return stringListToArray(arguments);
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    public String[] getJvmArgumentsArray() {
        return stringListToArray(jvmArguments);
    }

    public TaskOutputProcessor getStdOutListener(Project project) {
        return stdOutListener.startExecution(project);
    }

    public TaskOutputProcessor getStdErrListener(Project project) {
        return stdErrListener.startExecution(project);
    }

    public SingleExecutionOutputProcessor getStdOutListener() {
        return stdOutListener;
    }

    public SingleExecutionOutputProcessor getStdErrListener() {
        return stdErrListener;
    }

    public ContextAwareCommandFinalizer getSuccessfulCommandFinalizer() {
        return successfulCommandFinalizer;
    }

    public ContextAwareCommandCompleteListener getCommandFinalizer() {
        return commandFinalizer;
    }

    private static List<String> processList(List<String> strings, TaskVariableMap varReplaceMap) {
        List<String> result = new ArrayList<>(strings.size());
        for (String str: strings) {
            result.add(StandardTaskVariable.replaceVars(str, varReplaceMap));
        }
        return result;
    }

    public static GradleTaskDef.Builder createFromTemplate(
            TaskOutputDef outputDef,
            GradleCommandTemplate command,
            TaskVariableMap varReplaceMap) {
        ExceptionHelper.checkNotNullArgument(outputDef, "outputDef");
        ExceptionHelper.checkNotNullArgument(command, "command");
        ExceptionHelper.checkNotNullArgument(varReplaceMap, "varReplaceMap");

        GradleTaskDef.Builder builder = new Builder(outputDef, processList(command.getTasks(), varReplaceMap));
        builder.setCommandName(command.getDisplayName());
        builder.setArguments(processList(command.getArguments(), varReplaceMap));
        builder.setJvmArguments(processList(command.getJvmArguments(), varReplaceMap));
        builder.setNonBlocking(!command.isBlocking());
        builder.setCleanOutput(command.isBlocking());
        return builder;
    }

    private static SingleExecutionOutputProcessor outputProcessor(
            final TaskOutputProcessor processor,
            SingleExecutionOutputProcessor processorFactory) {

        final SingleExecutionOutputProcessor nullSafeProcessorFactory = processorFactory != null
                ? processorFactory
                : NoOpSingleExecutionOutputProcessor.INSTANCE;

        if (processor == null) {
            return nullSafeProcessorFactory;
        }

        return new SingleExecutionOutputProcessor() {
            @Override
            public TaskOutputProcessor startExecution(Project project) {
                return mergedOutputProcessor(
                        processor,
                        nullSafeProcessorFactory.startExecution(project));
            }
        };
    }

    private static TaskOutputProcessor mergedOutputProcessor(TaskOutputProcessor... processors) {
        final TaskOutputProcessor[] wrappedProcessors = processors.clone();
        return new TaskOutputProcessor() {
            @Override
            public void processLine(String line) {
                processByAll(wrappedProcessors, line);
            }
        };
    }

    private static void processByAll(TaskOutputProcessor[] processors, String line) {
        for (TaskOutputProcessor processor: processors) {
            try {
                if (processor != null) {
                    processor.processLine(line);
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Unexpected failure while checking a line of the output.", ex);
            }
        }
    }

    private static Object getTabKeyForCommand(GradleCommandTemplate command) {
        String displayName = command.getDisplayName();

        return displayName.isEmpty()
                ? new ArrayList<>(command.getTasks())
                : displayName;
    }

    private static TaskOutputDef getOutputDef(
            NbGradleProject project,
            TaskKind kind,
            GradleCommandTemplate command) {

        Object additionalKey;
        String caption;

        switch (kind) {
            case DEBUG:
                additionalKey = null;
                caption = project.getDisplayName() + " - debug";
                break;
            case RUN:
                additionalKey = null;
                caption = project.getDisplayName() + " - run";
                break;
            case BUILD:
                additionalKey = null;
                caption = project.getDisplayName();
                break;
            case OTHER:
                additionalKey = getTabKeyForCommand(command);
                caption = project.getDisplayName() + " - " + command.getSafeDisplayName();
                break;
            default:
                throw new AssertionError(kind.name());
        }

        TaskOutputKey outputKey = new TaskOutputKey(kind, project.getProjectDirectoryAsFile(), additionalKey);
        return new TaskOutputDef(outputKey, caption);
    }

    private static GradleTargetVerifier getGradleTargetVerifier(
            NbGradleProject project,
            Lookup actionContext,
            CustomCommandActions customActions) {

        ContextAwareGradleTargetVerifier contextAwareVerifier
                = customActions.getContextAwareGradleTargetVerifier();

        final GradleTargetVerifier verifier1 = contextAwareVerifier != null
                ? contextAwareVerifier.startCommand(project, actionContext)
                : null;
        final GradleTargetVerifier verifier2 = customActions.getGradleTargetVerifier();

        if (verifier1 == null) return verifier2;
        if (verifier2 == null) return verifier1;

        return new GradleTargetVerifier() {
            @Override
            public boolean checkTaskExecutable(
                    GradleTarget gradleTarget,
                    OutputWriter output,
                    OutputWriter errOutput) {
                return verifier1.checkTaskExecutable(gradleTarget, output, errOutput)
                        && verifier2.checkTaskExecutable(gradleTarget, output, errOutput);
            }
        };
    }

    private static void addAdditionalArguments(
            NbGradleProject project,
            Lookup actionContext,
            CustomCommandActions customActions,
            TaskVariableMap varReplaceMap,
            GradleTaskDef.Builder builder) {

        ContextAwareCommandArguments additional = customActions.getContextAwareCommandArguments();
        if (additional == null) {
            return;
        }
        List<String> argumentList = additional.getCommandArguments(project, actionContext);
        builder.addArguments(processList(argumentList, varReplaceMap));
    }

    public static GradleTaskDef.Builder createFromTemplate(
            NbGradleProject project,
            GradleCommandTemplate command,
            CustomCommandActions customActions,
            Lookup actionContext) {

        TaskVariableMap varReplaceMap = project.getVarReplaceMap(actionContext);
        TaskOutputDef caption = getOutputDef(project, customActions.getTaskKind(), command);
        GradleTaskDef.Builder builder = createFromTemplate(caption, command, varReplaceMap);
        addAdditionalArguments(project, actionContext, customActions, varReplaceMap, builder);

        builder.setNonUserTaskVariables(varReplaceMap);

        builder.setStdOutListener(outputProcessor(
                customActions.getStdOutProcessor(),
                customActions.getSingleExecutionStdOutProcessor()));

        builder.setStdErrListener(outputProcessor(
                customActions.getStdErrProcessor(),
                customActions.getSingleExecutionStdErrProcessor()));

        ContextAwareCommandAction contextAwareAction = customActions.getContextAwareAction();
        if (contextAwareAction != null) {
            ContextAwareCommandFinalizer finalizer = contextAwareAction.startCommand(project, actionContext);
            builder.setSuccessfulCommandFinalizer(finalizer);
        }

        ContextAwareCommandCompleteAction contextAwareFinalizer = customActions.getContextAwareFinalizer();
        if (contextAwareFinalizer != null) {
            ContextAwareCommandCompleteListener finalizer
                    = contextAwareFinalizer.startCommand(project, actionContext);
            builder.setCommandFinalizer(finalizer);
        }

        CommandExceptionHider exceptionHider = customActions.getCommandExceptionHider();
        if (exceptionHider != null) {
            builder.setCommandExceptionHider(exceptionHider);
        }

        builder.setGradleTargetVerifier(getGradleTargetVerifier(project, actionContext, customActions));

        return builder;
    }

    private enum NoOpTaskOutputProcessor implements TaskOutputProcessor {
        INSTANCE;

        @Override
        public void processLine(String line) {
        }
    }

    private enum NoOpSingleExecutionOutputProcessor implements SingleExecutionOutputProcessor {
        INSTANCE;

        @Override
        public TaskOutputProcessor startExecution(Project project) {
            return NoOpTaskOutputProcessor.INSTANCE;
        }
    }

    private enum NoOpSuccessfulFinalizer implements ContextAwareCommandFinalizer {
        INSTANCE;

        @Override
        public void finalizeSuccessfulCommand(OutputWriter output, OutputWriter errOutput) {
        }
    }

    private enum NoOpFinalizer implements ContextAwareCommandCompleteListener {
        INSTANCE;

        @Override
        public void onComplete(ExecutedCommandContext executedCommandContext, Throwable error) {
        }
    }

    private enum NoOpExceptionHider implements CommandExceptionHider {
        INSTANCE;

        @Override
        public boolean hideException(Throwable taskError) {
            return false;
        }
    }
}
