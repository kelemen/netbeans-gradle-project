package org.netbeans.gradle.project.tasks;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultCancellationTokenSource;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.model.util.TemporaryFileManager;
import org.netbeans.gradle.model.util.TemporaryFileRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.config.InitScriptQuery;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.ExecutedCommandContext;
import org.netbeans.gradle.project.api.task.GradleTargetVerifier;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.output.BuildErrorConsumer;
import org.netbeans.gradle.project.output.FileLineConsumer;
import org.netbeans.gradle.project.output.IOTabRef;
import org.netbeans.gradle.project.output.IOTabs;
import org.netbeans.gradle.project.output.InputOutputWrapper;
import org.netbeans.gradle.project.output.LineOutputWriter;
import org.netbeans.gradle.project.output.OutputLinkPrinter;
import org.netbeans.gradle.project.output.OutputUrlConsumer;
import org.netbeans.gradle.project.output.ProjectFileConsumer;
import org.netbeans.gradle.project.output.ReaderInputStream;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.netbeans.gradle.project.output.StackTraceConsumer;
import org.netbeans.gradle.project.output.TaskIOTab;
import org.netbeans.gradle.project.output.WriterOutputStream;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.spi.project.ui.support.BuildExecutionSupport;
import org.openide.LifecycleManager;
import org.openide.windows.OutputWriter;

public final class AsyncGradleTask implements Runnable {
    private static final TaskExecutor TASK_EXECUTOR
            = NbTaskExecutors.newExecutor("Gradle-Task-Executor", Integer.MAX_VALUE);
    private static final TaskExecutor CANCEL_EXECUTOR
            = NbTaskExecutors.newExecutor("Gradle-Cancel-Executor", Integer.MAX_VALUE);
    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final NbGradleProject project;
    private final GradleCommandSpecFactory taskDefFactroy;
    private final CommandCompleteListener listener;

    public AsyncGradleTask(
            NbGradleProject project,
            GradleCommandSpecFactory taskDefFactroy,
            CommandCompleteListener listener) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(taskDefFactroy, "taskDefFactroy");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.project = project;
        this.taskDefFactroy = taskDefFactroy;
        this.listener = listener;
    }

    public NbGradleProject getProject() {
        return project;
    }

    public GradleCommandSpecFactory getTaskDefFactroy() {
        return taskDefFactroy;
    }

    public CommandCompleteListener getListener() {
        return listener;
    }

    @Override
    public void run() {
        submitGradleTask(taskDefFactroy, listener);
    }

    private static void closeAll(List<? extends Closeable> toClose) {
        for (Closeable ref: toClose) {
            try {
                ref.close();
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to close reference: " + ref, ex);
            }
        }
    }

    private static List<TemporaryFileRef> getAllInitScriptFiles(NbGradleProject project) {
        if (GlobalGradleSettings.getOmitInitScript().getValue()) {
            return Collections.emptyList();
        }

        Collection<? extends InitScriptQuery> scriptQueries
                = project.getCombinedExtensionLookup().lookupAll(InitScriptQuery.class);

        List<TemporaryFileRef> results = new ArrayList<>(scriptQueries.size());
        try {
            for (InitScriptQuery scriptQuery: scriptQueries) {
                try {
                    String scriptContent = scriptQuery.getInitScript();
                    results.add(TemporaryFileManager.getDefault().createFile(
                            "task-init-script", scriptContent, UTF8));
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to create initialization script provided by " + scriptQuery.getClass().getName(),
                            ex);
                }
            }
            return results;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Failed to create initialization scripts.", ex);
            closeAll(results);
            return Collections.emptyList();
        }
    }

    private static void printCommand(OutputWriter buildOutput, String command, GradleTaskDef taskDef) {
        buildOutput.println(NbStrings.getExecutingTaskMessage(command));
        if (!taskDef.getArguments().isEmpty()) {
            buildOutput.println(NbStrings.getTaskArgumentsMessage(taskDef.getArguments()));
        }
        if (!taskDef.getJvmArguments().isEmpty()) {
            buildOutput.println(NbStrings.getTaskJvmArgumentsMessage(taskDef.getJvmArguments()));
        }

        buildOutput.println();
    }

    private static boolean hasArgumentInTaskNames(List<String> taskNames) {
        for (String taskName: taskNames) {
            if (taskName.startsWith("--")) {
                return true;
            }
        }
        return false;
    }

    private static void configureBuildLauncher(
            OperationInitializer targetSetup,
            BuildLauncher buildLauncher,
            GradleTaskDef taskDef,
            List<TemporaryFileRef> initScripts) {

        GradleModelLoader.setupLongRunningOP(targetSetup, buildLauncher);

        List<String> arguments = new LinkedList<>();
        arguments.addAll(taskDef.getArguments());

        for (TemporaryFileRef initScript: initScripts) {
            LOGGER.log(Level.INFO, "Applying init-script: {0}", initScript);
            arguments.add("--init-script");
            arguments.add(initScript.getFile().getPath());
        }

        // HACK: GRADLE-2972
        if (hasArgumentInTaskNames(taskDef.getTaskNames()) || arguments.contains("--tests")) {
            arguments.addAll(0, taskDef.getTaskNames());
            buildLauncher.withArguments(arguments.toArray(new String[arguments.size()]));
        }
        else {
            if (!arguments.isEmpty()) {
                buildLauncher.withArguments(arguments.toArray(new String[arguments.size()]));
            }

            buildLauncher.forTasks(taskDef.getTaskNamesArray());
        }
    }

    private static OutputRef configureOutput(
            NbGradleProject project,
            GradleTaskDef taskDef,
            BuildLauncher buildLauncher,
            TaskIOTab tab) {

        List<SmartOutputHandler.Consumer> outputConsumers = new LinkedList<>();
        outputConsumers.add(new OutputLinkPrinter(
                new StackTraceConsumer(project),
                new OutputUrlConsumer(),
                new ProjectFileConsumer(project)));

        List<SmartOutputHandler.Consumer> errorConsumers = new LinkedList<>();
        errorConsumers.add(new BuildErrorConsumer());
        errorConsumers.add(new OutputLinkPrinter(
                new StackTraceConsumer(project),
                new OutputUrlConsumer(),
                new ProjectFileConsumer(project),
                new FileLineConsumer()));

        InputOutputWrapper io = tab.getIo();
        Writer forwardedStdOut = new LineOutputWriter(new SmartOutputHandler(
                io.getIo(),
                io.getOutRef(),
                Arrays.asList(taskDef.getStdOutListener(project)),
                outputConsumers));
        Writer forwardedStdErr = new LineOutputWriter(new SmartOutputHandler(
                io.getIo(),
                io.getErrRef(),
                Arrays.asList(taskDef.getStdErrListener(project)),
                errorConsumers));

        buildLauncher.setStandardOutput(new WriterOutputStream(forwardedStdOut));
        buildLauncher.setStandardError(new WriterOutputStream(forwardedStdErr));
        buildLauncher.setStandardInput(new ReaderInputStream(tab.getIo().getInRef()));

        return new OutputRef(forwardedStdOut, forwardedStdErr);
    }

    private boolean checkTaskExecutable(
            ProjectConnection projectConnection,
            GradleTaskDef taskDef,
            GradleModelLoader.ModelBuilderSetup targetSetup,
            InputOutputWrapper io) {

        GradleTargetVerifier targetVerifier = taskDef.getGradleTargetVerifier();
        if (targetVerifier == null) {
            return true;
        }

        ModelBuilder<BuildEnvironment> envGetter = projectConnection.model(BuildEnvironment.class);
        GradleModelLoader.setupLongRunningOP(targetSetup, envGetter);

        BuildEnvironment buildEnv = envGetter.get();

        GradleTarget gradleTarget = new GradleTarget(
                    targetSetup.getJDKVersion(),
                    GradleVersion.version(buildEnv.getGradle().getGradleVersion()));

        return targetVerifier.checkTaskExecutable(gradleTarget, io.getOutRef(), io.getErrRef());
    }

    private GradleModelLoader.ModelBuilderSetup createTargetSetup(
            GradleTaskDef taskDef,
            ProgressHandle progress) {

        return new GradleModelLoader.ModelBuilderSetup(project,
                Collections.<String>emptyList(),
                taskDef.getJvmArguments(),
                progress);
    }

    private static void scheduleCancel(final DefaultCancellationTokenSource cancelSource) {
        CANCEL_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                cancelSource.cancel();
            }
        }, null);
    }

    private void runBuild(CancellationToken cancelToken, BuildLauncher buildLauncher) {
        // It is not possible to implement org.gradle.tooling.CancellationToken
        // Attempting to do so will cause Gradle to throw a class cast exception
        // somewhere.
        final DefaultCancellationTokenSource cancelSource = new DefaultCancellationTokenSource();
        buildLauncher.withCancellationToken(cancelSource.token());

        ListenerRef cancelListenerRef = cancelToken.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                scheduleCancel(cancelSource);
            }
        });
        try {
            buildLauncher.run();
        } catch (Throwable ex) {
            if (!cancelToken.isCanceled()) {
                throw ex;
            }
            else {
                // The exception is most likely due to cancellation.
                // Report cancellation message?
                LOGGER.log(Level.INFO, "Build has been canceled.", ex);
            }
        } finally {
            cancelListenerRef.unregister();
        }
    }

    private void doGradleTasksWithProgress(
            CancellationToken cancelToken,
            ProgressHandle progress,
            BuildExecutionItem buildItem) {

        GradleTaskDef taksDef = buildItem.getProcessedTaskDef();
        CancellationToken mergedToken = Cancellation.anyToken(
                cancelToken,
                taksDef.getCancelToken());
        doGradleTasksWithProgressIgnoreTaskDefCancel(mergedToken, progress, buildItem);
    }

    // TODO: This method is extremly nasty and is in a dire need of refactoring.
    private void doGradleTasksWithProgressIgnoreTaskDefCancel(
            CancellationToken cancelToken,
            final ProgressHandle progress,
            BuildExecutionItem buildItem) {

        GradleTaskDef taskDef = buildItem.getProcessedTaskDef();
        Objects.requireNonNull(taskDef, "command.processed");

        StringBuilder commandBuilder = new StringBuilder(128);
        commandBuilder.append("gradle");
        for (String task : taskDef.getTaskNames()) {
            commandBuilder.append(' ');
            commandBuilder.append(task);
        }

        String command = commandBuilder.toString();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "Executing: {0}, Args = {1}, JvmArg = {2}",
                    new Object[]{command, taskDef.getArguments(), taskDef.getJvmArguments()});
        }

        File projectDir = project.getProjectDirectoryAsFile();

        GradleModelLoader.ModelBuilderSetup targetSetup = createTargetSetup(taskDef, progress);

        Throwable commandError = null;

        GradleConnector gradleConnector = GradleModelLoader.createGradleConnector(cancelToken, project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            BuildLauncher buildLauncher = projectConnection.newBuild();
            List<TemporaryFileRef> initScripts = getAllInitScriptFiles(project);
            try {
                configureBuildLauncher(targetSetup, buildLauncher, taskDef, initScripts);

                TaskOutputDef outputDef = taskDef.getOutputDef();

                TaskOutputKey outputDefKey = outputDef.getKey();
                String outputDefCaption = outputDef.getCaption();
                try (IOTabRef<TaskIOTab> ioRef = IOTabs.taskTabs().getTab(outputDefKey, outputDefCaption)) {
                    TaskIOTab tab = ioRef.getTab();
                    tab.setLastTask(buildItem.getSourceTaskDef(), adjust(taskDef));
                    tab.taskStarted();
                    BuildExecutionSupport.registerRunningItem(buildItem);

                    try {
                        OutputWriter buildOutput = tab.getIo().getOutRef();
                        if (GlobalGradleSettings.getAlwaysClearOutput().getValue()
                                || taskDef.isCleanOutput()) {
                            buildOutput.reset();
                            // There is no need to reset buildErrOutput,
                            // at least this is what NetBeans tells you in its
                            // logs if you do.
                        }
                        printCommand(buildOutput, command, taskDef);

                        try (OutputRef outputRef = configureOutput(project, taskDef, buildLauncher, tab)) {
                            assert outputRef != null; // Avoid warning

                            InputOutputWrapper io = tab.getIo();
                            io.getIo().select();

                            if (checkTaskExecutable(projectConnection, taskDef, targetSetup, io)) {
                                runBuild(cancelToken, buildLauncher);

                                taskDef.getSuccessfulCommandFinalizer().finalizeSuccessfulCommand(
                                        buildOutput,
                                        io.getErrRef());
                            }
                        }
                    } catch (Throwable ex) {
                        Level logLevel;

                        if (taskDef.getCommandExceptionHider().hideException(ex)) {
                            logLevel = Level.INFO;
                        }
                        else {
                            commandError = ex;
                            logLevel = ex instanceof Exception ? Level.INFO : Level.SEVERE;
                        }

                        LOGGER.log(logLevel, "Gradle build failure: " + command, ex);

                        String buildFailureMessage = NbStrings.getBuildFailure(command);

                        OutputWriter buildErrOutput = tab.getIo().getErrRef();
                        buildErrOutput.println();
                        buildErrOutput.println(buildFailureMessage);
                        if (commandError != null) {
                            project.displayError(buildFailureMessage, commandError);
                        }
                    }

                    tab.taskCompleted();
                }
                buildItem.markFinished();
                BuildExecutionSupport.registerFinishedItem(buildItem);
            } finally {
                closeAll(initScripts);
            }
        } finally {
            try {
                if (projectConnection != null) {
                    projectConnection.close();
                }
            } finally {
                ExecutedCommandContext commandContext = buildItem.getCommandContext();
                taskDef.getCommandFinalizer().onComplete(commandContext, commandError);
            }
        }
    }

    private static void preSubmitGradleTask() {
        LifecycleManager.getDefault().saveAll();
    }

    private static void collectTaskVars(String[] strings, List<DisplayedTaskVariable> taskVars) {
        for (String str: strings) {
            StandardTaskVariable.replaceVars(str, EmptyTaskVarMap.INSTANCE, taskVars);
        }
    }

    private static TaskVariableMap queryVariablesNow(List<DisplayedTaskVariable> taskVars) {
        assert SwingUtilities.isEventDispatchThread();

        final Map<TaskVariable, DisplayedTaskVariable> names = new LinkedHashMap<>();
        for (DisplayedTaskVariable var: taskVars) {
            DisplayedTaskVariable currentValue = names.get(var.getVariable());
            if (currentValue == null || currentValue.isDefault()) {
                names.put(var.getVariable(), var);
            }
        }

        Map<DisplayedTaskVariable, String> varMap = TaskVariableQueryDialog.queryVariables(names.values());

        return varMap != null
                ? DisplayedTaskVariable.variableMap(varMap)
                : null;
    }

    private static TaskVariableMap queryVariables(final List<DisplayedTaskVariable> taskVars) {
        final AtomicReference<TaskVariableMap> result = new AtomicReference<>(null);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    result.set(queryVariablesNow(taskVars));
                }
            });
            return result.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return EmptyTaskVarMap.INSTANCE;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static GradleTaskDef queryUserDefinedInputOfTask(GradleTaskDef taskDef) {
        String[] taskNames = taskDef.getTaskNamesArray();
        String[] arguments = taskDef.getArgumentArray();
        String[] jvmArguments = taskDef.getJvmArgumentsArray();

        List<DisplayedTaskVariable> taskVars = new LinkedList<>();
        collectTaskVars(taskNames, taskVars);
        collectTaskVars(arguments, taskVars);
        collectTaskVars(jvmArguments, taskVars);

        if (taskVars.isEmpty()) {
            return taskDef;
        }

        TaskVariableMap varMap = queryVariables(taskVars);
        if (varMap == null) {
            return null;
        }

        replaceAllVars(taskNames, varMap);
        replaceAllVars(arguments, varMap);
        replaceAllVars(jvmArguments, varMap);

        GradleTaskDef.Builder result = new GradleTaskDef.Builder(taskDef);
        result.setTaskNames(Arrays.asList(taskNames));
        result.setArguments(Arrays.asList(arguments));
        result.setJvmArguments(Arrays.asList(jvmArguments));
        result.addNonUserTaskVariables(varMap);
        return result.create();
    }

    private static void replaceAllVars(String[] strings, TaskVariableMap varMap) {
        for (int i = 0; i < strings.length; i++) {
            strings[i] = StandardTaskVariable.replaceVars(strings[i], varMap);
        }
    }

    private static GradleTaskDef updateGradleTaskDef(GradleTaskDef taskDef) {
        List<String> globalJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();

        GradleTaskDef result;
        if (globalJvmArgs != null && !globalJvmArgs.isEmpty()) {
            GradleTaskDef.Builder builder = new GradleTaskDef.Builder(taskDef);
            builder.addJvmArguments(globalJvmArgs);
            result = builder.create();
        }
        else {
            result = taskDef;
        }

        return queryUserDefinedInputOfTask(result);
    }

    private void submitGradleTask(
            final GradleCommandSpecFactory taskDefFactory,
            final CommandCompleteListener listener) {
        preSubmitGradleTask();

        DaemonTaskDefFactory daemonTaskDefFactory = new DaemonTaskDefFactory() {
            @Override
            public String getDisplayName() {
                return taskDefFactory.getDisplayName();
            }

            @Override
            public DaemonTaskDef tryCreateTaskDef(CancellationToken cancelToken) throws Exception {
                GradleCommandSpec commandSpec = taskDefFactory.tryCreateCommandSpec(cancelToken);
                if (commandSpec == null) {
                    return null;
                }

                ProcessedCommandSpec processedSpec = tryCreateCommandSpec(commandSpec);
                return processedSpec != null
                        ? processedSpec.newBuildExecutionItem().getDaemonTaskDef()
                        : null;
            }
        };

        GradleDaemonManager.submitGradleTask(TASK_EXECUTOR, daemonTaskDefFactory, listener);
    }

    private AsyncGradleTask adjust(GradleTaskDef taskDef) {
        return adjust(GradleCommandSpec.adjustFactory(
                taskDefFactroy,
                taskDef.getTaskNames(),
                taskDef.getArguments(),
                taskDef.getJvmArguments()));
    }

    private AsyncGradleTask adjust(GradleCommandSpecFactory newFactory) {
        return new AsyncGradleTask(project, newFactory, listener);
    }

    private static GradleTaskDef createTaskDef(GradleCommandSpec commandSpec) {
        GradleTaskDef taskDef = commandSpec.getProcessed();
        if (taskDef == null) {
            taskDef = updateGradleTaskDef(commandSpec.getSource());
            if (taskDef == null) {
                LOGGER.log(Level.WARNING,
                        "Cannot process Gradle command template: {0}",
                        commandSpec.getSource().getSafeCommandName());
                return null;
            }
        }
        return taskDef;
    }

    private ProcessedCommandSpec tryCreateCommandSpec(GradleCommandSpec commandSpec) {
        GradleTaskDef taskDef = createTaskDef(commandSpec);
        return taskDef != null
                ? new ProcessedCommandSpec(commandSpec, taskDef)
                : null;
    }

    private class ProcessedCommandSpec {
        private final GradleCommandSpec commandSpec;
        private final String taskName;
        private final String progressCaption;

        public ProcessedCommandSpec(GradleCommandSpec commandSpec, GradleTaskDef taskDef) {
            assert commandSpec != null;
            assert taskDef != null;

            this.commandSpec = new GradleCommandSpec(commandSpec.getSource(), taskDef);
            this.taskName = taskDef.getSafeCommandName();
            this.progressCaption = NbStrings.getExecuteTasksText(taskName);
        }

        public GradleTaskDef getSourceTaskDef() {
            return commandSpec.getSource();
        }

        public GradleTaskDef getProcessedTaskDef() {
            return commandSpec.getProcessed();
        }

        public String getDisplayName() {
            // Note that the project name may change when reloading a project.
            return taskName + " " + project.getDisplayName();
        }

        public String getProgressCaption() {
            return progressCaption;
        }

        public BuildExecutionItem newBuildExecutionItem() {
            return new BuildExecutionItem(this);
        }
    }

    // TODO: (netbeans7.4): change to BuildExecutionSupport.ActionItem with ref to project
    private class BuildExecutionItem implements BuildExecutionSupport.Item {
        private final ProcessedCommandSpec processedCommandSpec;
        private final DaemonTaskDef daemonTaskDef;
        private volatile boolean running;

        public BuildExecutionItem(ProcessedCommandSpec processedCommandSpec) {
            assert processedCommandSpec != null;

            this.processedCommandSpec = processedCommandSpec;

            String progressCaption = processedCommandSpec.getProgressCaption();
            boolean nonBlocking = processedCommandSpec.getProcessedTaskDef().isNonBlocking();
            this.daemonTaskDef = new DaemonTaskDef(progressCaption, nonBlocking, new DaemonTask() {
                @Override
                public void run(CancellationToken cancelToken, ProgressHandle progress) {
                    doGradleTasksWithProgress(cancelToken, progress, BuildExecutionItem.this);
                }
            });
            this.running = true;
        }

        public DaemonTaskDef getDaemonTaskDef() {
            return daemonTaskDef;
        }

        public GradleTaskDef getSourceTaskDef() {
            return processedCommandSpec.getSourceTaskDef();
        }

        public GradleTaskDef getProcessedTaskDef() {
            return processedCommandSpec.getProcessedTaskDef();
        }

        public ExecutedCommandContext getCommandContext() {
            TaskVariableMap taskVariables = processedCommandSpec
                    .getProcessedTaskDef()
                    .getNonUserTaskVariables();
            return new ExecutedCommandContext(taskVariables);
        }

        @Override
        public String getDisplayName() {
            return processedCommandSpec.getDisplayName();
        }

        @Override
        public void repeatExecution() {
            DaemonTaskDef newTaskDef = processedCommandSpec.newBuildExecutionItem().getDaemonTaskDef();
            GradleDaemonManager.submitGradleTask(TASK_EXECUTOR, newTaskDef, listener);
        }

        public void markFinished() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public void stopRunning() {
            // no-op
        }
    }

    private static class OutputRef implements Closeable {
        private final Writer[] writers;

        public OutputRef(Writer... writers) {
            this.writers = writers.clone();

            ExceptionHelper.checkNotNullElements(this.writers, "writers");
        }

        @Override
        public void close() throws IOException {
            for (Writer writer: writers) {
                writer.close();
            }
        }
    }
}
