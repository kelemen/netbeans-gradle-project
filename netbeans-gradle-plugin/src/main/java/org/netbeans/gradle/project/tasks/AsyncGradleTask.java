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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.util.TemporaryFileManager;
import org.netbeans.gradle.model.util.TemporaryFileRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.InitScriptQuery;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.output.BuildErrorConsumer;
import org.netbeans.gradle.project.output.FileLineConsumer;
import org.netbeans.gradle.project.output.IOTabRef;
import org.netbeans.gradle.project.output.IOTabs;
import org.netbeans.gradle.project.output.LineOutputWriter;
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
import org.openide.util.RequestProcessor;
import org.openide.windows.OutputWriter;

public final class AsyncGradleTask implements Runnable {
    private static final RequestProcessor TASK_EXECUTOR
            = new RequestProcessor("Gradle-Task-Executor", 10, true);
    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final NbGradleProject project;
    private final Callable<GradleCommandSpec> taskDefFactroy;
    private final CommandCompleteListener listener;

    public AsyncGradleTask(
            NbGradleProject project,
            Callable<GradleCommandSpec> taskDefFactroy,
            CommandCompleteListener listener) {
        if (project == null)
            throw new NullPointerException("project");
        if (taskDefFactroy == null)
            throw new NullPointerException("taskDefFactroy");
        if (listener == null)
            throw new NullPointerException("listener");

        this.project = project;
        this.taskDefFactroy = taskDefFactroy;
        this.listener = listener;
    }

    public NbGradleProject getProject() {
        return project;
    }

    public Callable<GradleCommandSpec> getTaskDefFactroy() {
        return taskDefFactroy;
    }

    public CommandCompleteListener getListener() {
        return listener;
    }

    @Override
    public void run() {
        submitGradleTask(project, taskDefFactroy, listener);
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

        List<TemporaryFileRef> results = new ArrayList<TemporaryFileRef>(scriptQueries.size());
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

    private static void configureBuildLauncher(
            NbGradleProject project,
            BuildLauncher buildLauncher,
            GradleTaskDef taskDef,
            List<TemporaryFileRef> initScripts,
            final ProgressHandle progress) {

        File javaHome = GradleModelLoader.getScriptJavaHome(project);
        if (javaHome != null) {
            buildLauncher.setJavaHome(javaHome);
        }

        if (!taskDef.getJvmArguments().isEmpty()) {
            buildLauncher.setJvmArguments(taskDef.getJvmArgumentsArray());
        }

        List<String> arguments = new LinkedList<String>();
        arguments.addAll(taskDef.getArguments());

        for (TemporaryFileRef initScript: initScripts) {
            LOGGER.log(Level.INFO, "Applying init-script: {0}", initScript);
            arguments.add("--init-script");
            arguments.add(initScript.getFile().getPath());
        }

        if (!arguments.isEmpty()) {
            buildLauncher.withArguments(arguments.toArray(new String[arguments.size()]));
        }

        buildLauncher.addProgressListener(new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent pe) {
                progress.progress(pe.getDescription());
            }
        });

        buildLauncher.forTasks(taskDef.getTaskNamesArray());
    }

    private static OutputRef configureOutput(
            NbGradleProject project,
            GradleTaskDef taskDef,
            BuildLauncher buildLauncher,
            TaskIOTab tab) {

        List<SmartOutputHandler.Consumer> consumers = new LinkedList<SmartOutputHandler.Consumer>();
        consumers.add(new StackTraceConsumer(project));
        consumers.add(new OutputUrlConsumer());
        consumers.add(new ProjectFileConsumer(project));

        List<SmartOutputHandler.Consumer> outputConsumers = new LinkedList<SmartOutputHandler.Consumer>();
        outputConsumers.addAll(consumers);

        List<SmartOutputHandler.Consumer> errorConsumers = new LinkedList<SmartOutputHandler.Consumer>();
        errorConsumers.add(new BuildErrorConsumer());
        errorConsumers.addAll(consumers);
        errorConsumers.add(new FileLineConsumer());

        Writer forwardedStdOut = new LineOutputWriter(new SmartOutputHandler(
                tab.getIo().getOutRef(),
                Arrays.asList(taskDef.getStdOutListener()),
                outputConsumers));
        Writer forwardedStdErr = new LineOutputWriter(new SmartOutputHandler(
                tab.getIo().getErrRef(),
                Arrays.asList(taskDef.getStdErrListener()),
                errorConsumers));

        buildLauncher.setStandardOutput(new WriterOutputStream(forwardedStdOut));
        buildLauncher.setStandardError(new WriterOutputStream(forwardedStdErr));
        buildLauncher.setStandardInput(new ReaderInputStream(tab.getIo().getInRef()));

        return new OutputRef(forwardedStdOut, forwardedStdErr);
    }

    private void doGradleTasksWithProgress(
            final ProgressHandle progress,
            BuildExecutionItem buildItem) {

        GradleTaskDef taskDef = buildItem.commandSpec.getProcessed();
        if (taskDef == null) throw new NullPointerException("command.processed");

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

        GradleConnector gradleConnector = GradleModelLoader.createGradleConnector(project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            BuildLauncher buildLauncher = projectConnection.newBuild();
            List<TemporaryFileRef> initScripts = getAllInitScriptFiles(project);
            try {
                configureBuildLauncher(project, buildLauncher, taskDef, initScripts, progress);

                TaskOutputDef outputDef = taskDef.getOutputDef();

                IOTabRef<TaskIOTab> ioRef
                        = IOTabs.taskTabs().getTab(outputDef.getKey(), outputDef.getCaption());
                
                try {
                    TaskIOTab tab = ioRef.getTab();
                    tab.setLastTask(buildItem.commandSpec.getSource(), adjust(taskDef));
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

                        OutputRef outputRef = configureOutput(project, taskDef, buildLauncher, tab);
                        try {
                            tab.getIo().getIo().select();
                            buildLauncher.run();

                            taskDef.getCommandFinalizer().finalizeSuccessfulCommand(
                                    buildOutput,
                                    tab.getIo().getErrRef());
                        } finally {
                            // This close method will only forward the last lines
                            // if they were not terminated with a line separator.
                            outputRef.close();
                        }
                    } catch (Throwable ex) {
                        LOGGER.log(
                                ex instanceof Exception ? Level.INFO : Level.SEVERE,
                                "Gradle build failure: " + command,
                                ex);

                        String buildFailureMessage = NbStrings.getBuildFailure(command);

                        OutputWriter buildErrOutput = tab.getIo().getErrRef();
                        buildErrOutput.println();
                        buildErrOutput.println(buildFailureMessage);
                        project.displayError(buildFailureMessage, ex, false);
                    }

                    tab.taskCompleted();
                } finally {
                    ioRef.close();
                }
                buildItem.markFinished();
                BuildExecutionSupport.registerFinishedItem(buildItem);
            } finally {
                closeAll(initScripts);
            }
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
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

        final Map<TaskVariable, DisplayedTaskVariable> names = new LinkedHashMap<TaskVariable, DisplayedTaskVariable>();
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
        final AtomicReference<TaskVariableMap> result = new AtomicReference<TaskVariableMap>(null);
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

        List<DisplayedTaskVariable> taskVars = new LinkedList<DisplayedTaskVariable>();
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
        return result.create();
    }

    private static void replaceAllVars(String[] strings, TaskVariableMap varMap) {
        for (int i = 0; i < strings.length; i++) {
            strings[i] = StandardTaskVariable.replaceVars(strings[i], varMap);
        }
    }

    private GradleTaskDef updateGradleTaskDef(GradleTaskDef taskDef) {
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
            final NbGradleProject project,
            final Callable<GradleCommandSpec> taskDefFactory,
            final CommandCompleteListener listener) {
        preSubmitGradleTask();

        Callable<DaemonTaskDef> daemonTaskDefFactory = new Callable<DaemonTaskDef>() {
            @Override
            public DaemonTaskDef call() throws Exception {
                GradleCommandSpec commandSpec = taskDefFactory.call();
                if (commandSpec == null) {
                    return null;
                }

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

                final GradleCommandSpec processedCommand = new GradleCommandSpec(commandSpec.getSource(), taskDef);

                String taskName = taskDef.getSafeCommandName();
                String caption = NbStrings.getExecuteTasksText(taskName);
                boolean nonBlocking = taskDef.isNonBlocking();
                
                final BuildExecutionItem buildItem = new BuildExecutionItem(project, processedCommand, caption, nonBlocking);

                return new DaemonTaskDef(caption, nonBlocking, new DaemonTask() {
                    @Override
                    public void run(ProgressHandle progress) {
                        doGradleTasksWithProgress(progress, buildItem);
                    }
                });
            }
        };

        GradleDaemonManager.submitGradleTask(TASK_EXECUTOR, daemonTaskDefFactory, listener);
    }

    private AsyncGradleTask adjust(GradleTaskDef taskDef) {
        return adjust(new CommandAdjusterFactory(taskDefFactroy, taskDef));
    }

    private AsyncGradleTask adjust(Callable<GradleCommandSpec> newFactory) {
        return new AsyncGradleTask(project, newFactory, listener);
    }

    // TODO(netbeans7.4): change to BuildExecutionSupport.ActionItem with ref to project
    private class BuildExecutionItem implements BuildExecutionSupport.Item {
        private final NbGradleProject project;
        private final GradleCommandSpec commandSpec;
        private final String caption;
        private final boolean nonBlocking;
        
        private volatile boolean running = true;

        public BuildExecutionItem(NbGradleProject project, GradleCommandSpec commandSpec, String caption, boolean nonBlocking) {
            this.project = project;
            this.commandSpec = commandSpec;
            this.caption = caption;
            this.nonBlocking = nonBlocking;
        }
        
        @Override
        public String getDisplayName() {
            return caption;
        }

        @Override
        public void repeatExecution() {
            GradleDaemonManager.submitGradleTask(
                    TASK_EXECUTOR, 
                    new DaemonTaskDef(caption, nonBlocking, new DaemonTask() {
                        @Override
                        public void run(ProgressHandle progress) {
                            doGradleTasksWithProgress(progress, BuildExecutionItem.this);
                        }
                    }), 
                    listener);
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

    private static final class CommandAdjusterFactory implements Callable<GradleCommandSpec> {
        private final Callable<GradleCommandSpec> source;
        private final List<String> taskNames;
        private final List<String> arguments;
        private final List<String> jvmArguments;

        public CommandAdjusterFactory(Callable<GradleCommandSpec> source, GradleTaskDef taskDef) {
            this.source = source;
            this.taskNames = taskDef.getTaskNames();
            this.arguments = taskDef.getArguments();
            this.jvmArguments = taskDef.getJvmArguments();
        }

        @Override
        public GradleCommandSpec call() throws Exception {
            GradleCommandSpec original = source.call();
            if (original == null) {
                return null;
            }

            GradleTaskDef.Builder result = new GradleTaskDef.Builder(original.getSource());
            result.setTaskNames(taskNames);
            result.setArguments(arguments);
            result.setJvmArguments(jvmArguments);

            return new GradleCommandSpec(original.getSource(), result.create());
        }
    }

    private static class OutputRef implements Closeable {
        private final Writer[] writers;

        public OutputRef(Writer... writers) {
            this.writers = writers.clone();
            for (Writer writer: this.writers) {
                if (writer == null) throw new NullPointerException("writer");
            }
        }

        @Override
        public void close() throws IOException {
            for (Writer writer: writers) {
                writer.close();
            }
        }
    }
}
