package org.netbeans.gradle.project.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.task.CommandExceptionHider;
import org.netbeans.gradle.project.api.task.ContextAwareCommandAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteAction;
import org.netbeans.gradle.project.api.task.ContextAwareCommandCompleteListener;
import org.netbeans.gradle.project.api.task.ContextAwareCommandFinalizer;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.GradleTargetVerifier;
import org.netbeans.gradle.project.api.task.TaskKind;
import org.netbeans.gradle.project.api.task.TaskOutputProcessor;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.openide.util.Lookup;
import org.openide.windows.OutputWriter;

public final class GradleTaskDef {
    public static final class Builder {
        private String commandName;
        private TaskOutputDef outputDef;
        private List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArguments;

        private SmartOutputHandler.Visitor stdOutListener;
        private SmartOutputHandler.Visitor stdErrListener;
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
            if (outputDef == null) throw new NullPointerException("outputDef");

            this.commandName = "";
            this.outputDef = outputDef;
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();
            this.stdOutListener = NoOpTaskOutputListener.INSTANCE;
            this.stdErrListener = NoOpTaskOutputListener.INSTANCE;
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
            if (nonUserTaskVariables == null) throw new NullPointerException("nonUserTaskVariables");

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
            if (commandExceptionHider == null) throw new NullPointerException("commandExceptionHider");
            this.commandExceptionHider = commandExceptionHider;
        }

        public void setNonUserTaskVariables(TaskVariableMap nonUserTaskVariables) {
            if (nonUserTaskVariables == null) throw new NullPointerException("nonUserTaskVariables");
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
            if (commandName == null) throw new NullPointerException("commandName");
            this.commandName = commandName;
        }

        public TaskOutputDef getOutputDef() {
            return outputDef;
        }

        public void setOutputDef(TaskOutputDef outputDef) {
            if (outputDef == null) throw new NullPointerException("outputDef");
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
            List<T> result = new ArrayList<T>(list1.size() + list2.size());
            result.addAll(list1);
            result.addAll(list2);
            for (T element: result) {
                if (element == null) throw new NullPointerException("element");
            }
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

        public SmartOutputHandler.Visitor getStdOutListener() {
            return stdOutListener;
        }

        public void setStdOutListener(SmartOutputHandler.Visitor stdOutListener) {
            if (stdOutListener == null) throw new NullPointerException("stdOutListener");
            this.stdOutListener = stdOutListener;
        }

        public SmartOutputHandler.Visitor getStdErrListener() {
            return stdErrListener;
        }

        public void setStdErrListener(SmartOutputHandler.Visitor stdErrListener) {
            if (stdErrListener == null) throw new NullPointerException("stdErrListener");
            this.stdErrListener = stdErrListener;
        }

        public ContextAwareCommandFinalizer getSuccessfulCommandFinalizer() {
            return successfulCommandFinalizer;
        }

        public void setSuccessfulCommandFinalizer(ContextAwareCommandFinalizer successfulCommandFinalizer) {
            if (successfulCommandFinalizer == null) throw new NullPointerException("successfulCommandFinalizer");
            this.successfulCommandFinalizer = successfulCommandFinalizer;
        }

        public ContextAwareCommandCompleteListener getCommandFinalizer() {
            return commandFinalizer;
        }

        public void setCommandFinalizer(ContextAwareCommandCompleteListener commandFinalizer) {
            if (commandFinalizer == null) throw new NullPointerException("commandFinalizer");
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
    private final SmartOutputHandler.Visitor stdOutListener;
    private final SmartOutputHandler.Visitor stdErrListener;
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

    public SmartOutputHandler.Visitor getStdOutListener() {
        return stdOutListener;
    }

    public SmartOutputHandler.Visitor getStdErrListener() {
        return stdErrListener;
    }

    public ContextAwareCommandFinalizer getSuccessfulCommandFinalizer() {
        return successfulCommandFinalizer;
    }

    public ContextAwareCommandCompleteListener getCommandFinalizer() {
        return commandFinalizer;
    }

    private static List<String> processList(List<String> strings, TaskVariableMap varReplaceMap) {
        List<String> result = new ArrayList<String>(strings.size());
        for (String str: strings) {
            result.add(StandardTaskVariable.replaceVars(str, varReplaceMap));
        }
        return result;
    }

    public static GradleTaskDef.Builder createFromTemplate(
            TaskOutputDef outputDef,
            GradleCommandTemplate command,
            TaskVariableMap varReplaceMap) {
        if (outputDef == null) throw new NullPointerException("outputDef");
        if (command == null) throw new NullPointerException("command");
        if (varReplaceMap == null) throw new NullPointerException("varReplaceMap");

        GradleTaskDef.Builder builder = new Builder(outputDef, processList(command.getTasks(), varReplaceMap));
        builder.setCommandName(command.getDisplayName());
        builder.setArguments(processList(command.getArguments(), varReplaceMap));
        builder.setJvmArguments(processList(command.getJvmArguments(), varReplaceMap));
        builder.setNonBlocking(!command.isBlocking());
        builder.setCleanOutput(command.isBlocking());
        return builder;
    }

    private static SmartOutputHandler.Visitor outputProcessorToLineVisitor(final TaskOutputProcessor processor) {
        if (processor == null) {
            return NoOpTaskOutputListener.INSTANCE;
        }

        return new SmartOutputHandler.Visitor() {
            @Override
            public void visitLine(String line) {
                processor.processLine(line);
            }
        };
    }

    private static Object getTabKeyForCommand(GradleCommandTemplate command) {
        String displayName = command.getDisplayName();

        return displayName.isEmpty()
                ? new ArrayList<String>(command.getTasks())
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

    public static GradleTaskDef.Builder createFromTemplate(
            NbGradleProject project,
            GradleCommandTemplate command,
            CustomCommandActions customActions,
            Lookup actionContext) {

        TaskVariableMap varReplaceMap = project.getVarReplaceMap(actionContext);
        TaskOutputDef caption = getOutputDef(project, customActions.getTaskKind(), command);
        GradleTaskDef.Builder builder = createFromTemplate(caption, command, varReplaceMap);

        builder.setNonUserTaskVariables(varReplaceMap);

        TaskOutputProcessor stdOutProcessor = customActions.getStdOutProcessor();
        builder.setStdOutListener(outputProcessorToLineVisitor(stdOutProcessor));

        TaskOutputProcessor stdErrProcessor = customActions.getStdErrProcessor();
        builder.setStdErrListener(outputProcessorToLineVisitor(stdErrProcessor));

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

        builder.setGradleTargetVerifier(customActions.getGradleTargetVerifier());

        return builder;
    }

    private enum NoOpTaskOutputListener implements SmartOutputHandler.Visitor {
        INSTANCE;

        @Override
        public void visitLine(String line) {
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
