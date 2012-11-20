package org.netbeans.gradle.project.tasks;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.output.BuildErrorConsumer;
import org.netbeans.gradle.project.output.FileLineConsumer;
import org.netbeans.gradle.project.output.InputOutputManager;
import org.netbeans.gradle.project.output.InputOutputManager.IORef;
import org.netbeans.gradle.project.output.LineOutputWriter;
import org.netbeans.gradle.project.output.OutputUrlConsumer;
import org.netbeans.gradle.project.output.ProjectFileConsumer;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.netbeans.gradle.project.output.StackTraceConsumer;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.openide.util.io.ReaderInputStream;
import org.openide.windows.OutputWriter;

public final class GradleTasks {
    private static final RequestProcessor TASK_EXECUTOR
            = new RequestProcessor("Gradle-Task-Executor", 10, true);

    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @StaticResource
    private static final String INIT_SCRIPT_PATH = "org/netbeans/gradle/project/resources/nb-init-script.gradle";

    private static final AtomicReference<String> INIT_SCRIPT = new AtomicReference<String>(null);

    private static String getInitScript() {
        String result = INIT_SCRIPT.get();
        if (result == null) {
            try {
                result = StringUtils.getResourceAsString(INIT_SCRIPT_PATH, Charset.forName("UTF-8"));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Missing init script", ex);
            }

            INIT_SCRIPT.compareAndSet(null, result);
            result = INIT_SCRIPT.get();
        }
        return result;
    }

    private static void writeToFile(String str, File file) throws IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(str.getBytes(UTF8));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static File getInitScriptFile() {
        if (GlobalGradleSettings.getOmitInitScript().getValue()) {
            return null;
        }

        try {
            File tmpFile = File.createTempFile("nb-gradle-init", ".gradle");
            try {
                writeToFile(getInitScript(), tmpFile);
            } catch (Throwable ex) {
                tmpFile.delete();
                throw ex;
            }
            return tmpFile;
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, "Failed to create initialization script.", ex);
        }
        return null;
    }

    private static File getJavaHome(NbGradleProject project) {
        JavaPlatform platform = project.getProperties().getPlatform().getValue();
        FileObject jdkHomeObj = platform != null
                ? GlobalGradleSettings.getHomeFolder(platform)
                : null;
        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
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
            File initScript,
            final ProgressHandle progress) {

        File javaHome = getJavaHome(project);
        if (javaHome != null) {
            buildLauncher.setJavaHome(javaHome);
        }

        if (!taskDef.getJvmArguments().isEmpty()) {
            buildLauncher.setJvmArguments(taskDef.getJvmArgumentsArray());
        }

        List<String> arguments = new LinkedList<String>();
        arguments.addAll(taskDef.getArguments());

        if (initScript != null) {
            arguments.add("--init-script");
            arguments.add(initScript.getPath());
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
            Reader buildIn,
            OutputWriter buildOutput,
            OutputWriter buildErrOutput) {

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
                buildOutput,
                Arrays.asList(taskDef.getStdOutListener()),
                outputConsumers));
        Writer forwardedStdErr = new LineOutputWriter(new SmartOutputHandler(
                buildErrOutput,
                Arrays.asList(taskDef.getStdErrListener()),
                errorConsumers));

        buildLauncher.setStandardOutput(new WriterOutputStream(forwardedStdOut));
        buildLauncher.setStandardError(new WriterOutputStream(forwardedStdErr));
        
        try {
            buildLauncher.setStandardInput(new ReaderInputStream(buildIn));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not create input stream", e);
        }

        return new OutputRef(forwardedStdOut, forwardedStdErr);
    }

    private static void doGradleTasksWithProgress(
            final ProgressHandle progress,
            NbGradleProject project,
            GradleTaskDef taskDef) {
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

        FileObject projectDir = project.getProjectDirectory();

        GradleConnector gradleConnector = GradleModelLoader.createGradleConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            BuildLauncher buildLauncher = projectConnection.newBuild();
            File initScript = getInitScriptFile();
            try {
                configureBuildLauncher(project, buildLauncher, taskDef, initScript, progress);

                IORef ioRef = InputOutputManager.getInputOutput(
                        taskDef.getCaption(),
                        taskDef.isReuseOutput(),
                        taskDef.isCleanOutput());
                try {
                    OutputWriter buildOutput = ioRef.getIo().getOut();
                    try {
                        OutputWriter buildErrOutput = ioRef.getIo().getErr();
                        try {
                            if (GlobalGradleSettings.getAlwaysClearOutput().getValue()
                                    || taskDef.isCleanOutput()) {
                                buildOutput.reset();
                                // There is no need to reset buildErrOutput,
                                // at least this is what NetBeans tells you in its
                                // logs if you do.
                            }

                            printCommand(buildOutput, command, taskDef);

                            OutputRef outputRef = configureOutput(
                                    project, taskDef, buildLauncher, ioRef.getIo().getIn(), buildOutput, buildErrOutput);
                            try {
                                ioRef.getIo().select();
                                buildLauncher.run();
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
                            buildErrOutput.println();
                            buildErrOutput.println(buildFailureMessage);
                            project.displayError(buildFailureMessage, ex, false);
                        } finally {
                            buildErrOutput.close();
                        }
                    } finally {
                        buildOutput.close();
                    }
                } finally {
                    ioRef.close();
                }
            } finally {
                if (initScript != null) {
                    initScript.delete();
                }
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

    private static void submitGradleTask(
            final NbGradleProject project,
            final Callable<GradleTaskDef> taskDefFactory) {
        preSubmitGradleTask();

        Callable<DaemonTaskDef> daemonTaskDefFactory = new Callable<DaemonTaskDef>() {
            @Override
            public DaemonTaskDef call() throws Exception {
                GradleTaskDef taskDef = taskDefFactory.call();
                if (taskDef == null) {
                    return null;
                }

                List<String> globalJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();

                final GradleTaskDef newTaskDef;
                if (globalJvmArgs != null && !globalJvmArgs.isEmpty()) {
                    GradleTaskDef.Builder builder = new GradleTaskDef.Builder(taskDef);
                    builder.addJvmArguments(globalJvmArgs);
                    newTaskDef = builder.create();
                }
                else {
                    newTaskDef = taskDef;
                }

                String caption = NbStrings.getExecuteTasksText(newTaskDef.getTaskNames());
                boolean nonBlocking = newTaskDef.isNonBlocking();

                return new DaemonTaskDef(caption, nonBlocking, new DaemonTask() {
                    @Override
                    public void run(ProgressHandle progress) {
                        doGradleTasksWithProgress(progress, project, newTaskDef);
                    }
                });
            }
        };

        GradleDaemonManager.submitGradleTask(TASK_EXECUTOR, daemonTaskDefFactory);
    }

    public static Runnable createAsyncGradleTask(NbGradleProject project, final GradleTaskDef taskDef) {
        if (taskDef == null) throw new NullPointerException("taskDef");
        return createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return taskDef;
            }
        });
    }

    public static Runnable createAsyncGradleTask(NbGradleProject project, Callable<GradleTaskDef> taskDefFactory) {
        return new AsyncGradleTask(project, taskDefFactory);
    }

    private static class AsyncGradleTask implements Runnable {
        private final NbGradleProject project;
        private final Callable<GradleTaskDef> taskDefFactroy;

        public AsyncGradleTask(NbGradleProject project, Callable<GradleTaskDef> taskDefFactroy) {
            if (project == null) throw new NullPointerException("project");
            if (taskDefFactroy == null) throw new NullPointerException("taskDefFactroy");

            this.project = project;
            this.taskDefFactroy = taskDefFactroy;
        }

        @Override
        public void run() {
            submitGradleTask(project, taskDefFactroy);
        }
    }

    private static class WriterOutputStream extends OutputStream {
        private final Writer writer;
        private final Charset encoding;

        public WriterOutputStream(Writer writer, Charset encoding) {
            if (writer == null) throw new NullPointerException("writer");
            if (encoding == null) throw new NullPointerException("encoding");
            this.writer = writer;
            this.encoding = encoding;
        }

        public WriterOutputStream(Writer writer) {
            this(writer, Charset.defaultCharset());
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            writer.write(new String(b, encoding));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            writer.write(new String(b, off, len, encoding));
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte)b});
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

    private GradleTasks() {
        throw new AssertionError();
    }
}
