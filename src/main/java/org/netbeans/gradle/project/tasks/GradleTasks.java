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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.StringUtils;
import org.netbeans.gradle.project.api.config.InitScriptQuery;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
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
import org.openide.util.RequestProcessor;
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

    private static File getInitScriptFile(InitScriptQuery scriptQuery) throws Throwable {
        File tmpFile = File.createTempFile("nb-gradle-init", ".gradle");
        try {
            writeToFile(scriptQuery.getInitScript(), tmpFile);
        } catch (Throwable ex) {
            if (!tmpFile.delete()) {
                LOGGER.log(Level.WARNING, "Failed to delete temporary file after a failure: {0}", tmpFile);
            }
            throw ex;
        }
        return tmpFile;
    }

    private static void deleteAllFiles(List<? extends File> files) {
        for (File file: files) {
            try {
                if (!file.delete()) {
                    throw new IOException("Failed to remove file " + file);
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to remove temporary file: " + file, ex);
            }
        }
    }

    private static List<File> getAllInitScriptFiles(NbGradleProject project) {
        if (GlobalGradleSettings.getOmitInitScript().getValue()) {
            return Collections.emptyList();
        }

        Collection<? extends InitScriptQuery> scriptQueries
                = project.getLookup().lookupAll(InitScriptQuery.class);

        List<File> results = new ArrayList<File>(scriptQueries.size());
        try {
            for (InitScriptQuery scriptQuery: scriptQueries) {
                try {
                    results.add(getInitScriptFile(scriptQuery));
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to create initialization script provided by " + scriptQuery.getClass().getName(),
                            ex);
                }
            }
            return results;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Failed to create initialization scripts.", ex);
            deleteAllFiles(results);
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
            List<File> initScripts,
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

        for (File initScript: initScripts) {
            LOGGER.log(Level.INFO, "Applying init-script: {0}", initScript);
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
            IORef buildIo) {

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
                buildIo.getOutRef(),
                Arrays.asList(taskDef.getStdOutListener()),
                outputConsumers));
        Writer forwardedStdErr = new LineOutputWriter(new SmartOutputHandler(
                buildIo.getErrRef(),
                Arrays.asList(taskDef.getStdErrListener()),
                errorConsumers));

        buildLauncher.setStandardOutput(new WriterOutputStream(forwardedStdOut));
        buildLauncher.setStandardError(new WriterOutputStream(forwardedStdErr));
        buildLauncher.setStandardInput(new ReaderInputStream(buildIo.getInRef()));

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

        File projectDir = project.getProjectDirectoryAsFile();

        GradleConnector gradleConnector = GradleModelLoader.createGradleConnector(project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            BuildLauncher buildLauncher = projectConnection.newBuild();
            List<File> initScripts = getAllInitScriptFiles(project);
            try {
                configureBuildLauncher(project, buildLauncher, taskDef, initScripts, progress);

                IORef ioRef = InputOutputManager.getInputOutput(
                        taskDef.getCaption(),
                        taskDef.isReuseOutput(),
                        taskDef.isCleanOutput());
                try {
                    try {
                        OutputWriter buildOutput = ioRef.getOutRef();
                        if (GlobalGradleSettings.getAlwaysClearOutput().getValue()
                                || taskDef.isCleanOutput()) {
                            buildOutput.reset();
                            // There is no need to reset buildErrOutput,
                            // at least this is what NetBeans tells you in its
                            // logs if you do.
                        }
                        printCommand(buildOutput, command, taskDef);

                        OutputRef outputRef = configureOutput(project, taskDef, buildLauncher, ioRef);
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

                        OutputWriter buildErrOutput = ioRef.getErrRef();
                        buildErrOutput.println();
                        buildErrOutput.println(buildFailureMessage);
                        project.displayError(buildFailureMessage, ex, false);
                    }
                } finally {
                    ioRef.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unexpected I/O exception.", ex);
            } finally {
                deleteAllFiles(initScripts);
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
            final Callable<GradleTaskDef> taskDefFactory,
            final CommandCompleteListener listener) {
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

        GradleDaemonManager.submitGradleTask(TASK_EXECUTOR, daemonTaskDefFactory, listener);
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            Callable<GradleTaskDef> taskDefFactory) {
        return new AsyncGradleTask(project, taskDefFactory, projectTaskCompleteListener(project));
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            Callable<GradleTaskDef> taskDefFactory,
            CommandCompleteListener listener) {
        return new AsyncGradleTask(project, taskDefFactory, listener);
    }

    public static CommandCompleteListener projectTaskCompleteListener(final NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        return new CommandCompleteListener() {
            @Override
            public void onComplete(Throwable error) {
                if (error != null) {
                    LOGGER.log(error instanceof Exception ? Level.INFO : Level.SEVERE,
                            "Gradle build failure.",
                            error);

                    String buildFailureMessage = NbStrings.getGradleTaskFailure();
                    project.displayError(buildFailureMessage, error, false);
                }
            }
        };
    }

    private static class AsyncGradleTask implements Runnable {
        private final NbGradleProject project;
        private final Callable<GradleTaskDef> taskDefFactroy;
        private final CommandCompleteListener listener;

        public AsyncGradleTask(
                NbGradleProject project,
                Callable<GradleTaskDef> taskDefFactroy,
                CommandCompleteListener listener) {
            if (project == null) throw new NullPointerException("project");
            if (taskDefFactroy == null) throw new NullPointerException("taskDefFactroy");
            if (listener == null) throw new NullPointerException("listener");

            this.project = project;
            this.taskDefFactroy = taskDefFactroy;
            this.listener = listener;
        }

        @Override
        public void run() {
            submitGradleTask(project, taskDefFactroy, listener);
        }
    }

    private static class ReaderInputStream extends InputStream {
        private final Reader reader;
        private final Charset encoding;
        private final AtomicReference<byte[]> cacheRef;

        public ReaderInputStream(Reader reader) {
            this(reader, Charset.defaultCharset());
        }

        public ReaderInputStream(Reader reader, Charset encoding) {
            if (reader == null) throw new NullPointerException("reader");
            if (encoding == null) throw new NullPointerException("encoding");

            this.reader = reader;
            this.encoding = encoding;
            this.cacheRef = new AtomicReference<byte[]>(new byte[0]);
        }

        private int readFromCache(byte[] b, int offset, int length) {
            byte[] cache;
            byte[] newCache;
            int toRead;
            do {
                cache = cacheRef.get();
                toRead = Math.min(cache.length, length);
                System.arraycopy(cache, 0, b, offset, toRead);
                newCache = new byte[cache.length - toRead];
                System.arraycopy(cache, toRead, newCache, 0, newCache.length);
            } while (!cacheRef.compareAndSet(cache, newCache));

            return toRead;
        }

        private boolean readToCache(int requiredBytes) throws IOException {
            assert requiredBytes > 0;
            // We rely on the encoder to choose the number of bytes to read but
            // it does not have to be actually accurate, it only matters
            // performance wise but this is not a performance critical code.

            CharsetEncoder encoder = encoding.newEncoder();
            int toRead = (int)((float)requiredBytes / encoder.averageBytesPerChar()) + 1;
            toRead = Math.max(toRead, requiredBytes);

            char[] readChars = new char[toRead];
            int readCount = reader.read(readChars);
            if (readCount <= 0) {
                // readCount should never be zero but if reader returns zero
                // regardless, assume that it believes that EOF has been
                // reached.
                return false;
            }

            ByteBuffer encodedBuffer = encoder.encode(CharBuffer.wrap(readChars, 0, readCount));
            byte[] encoded = new byte[encodedBuffer.remaining()];
            encodedBuffer.get(encoded);

            byte[] oldCache;
            byte[] newCache;

            do {
                oldCache = cacheRef.get();
                newCache = new byte[oldCache.length + encoded.length];
                System.arraycopy(oldCache, 0, newCache, 0, oldCache.length);
                System.arraycopy(encoded, 0, newCache, oldCache.length, encoded.length);
            } while (!cacheRef.compareAndSet(oldCache, newCache));
            return true;
        }

        @Override
        public int read() throws IOException {
            byte[] result = new byte[1];
            if (read(result) <= 0) {
                // Althouth the above read should never return zero.
                return -1;
            }
            else {
                return ((int)result[0] & 0xFF);
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // Note that while this method is implemented to be thread-safe
            // calling it concurrently is unadvised, since read is not atomic.
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int currentOffset = off;
            int currentLength = len;
            int readCount = 0;
            do {
                int currentRead = readFromCache(b, currentOffset, currentLength);
                readCount += currentRead;
                currentOffset += currentRead;
                currentLength -= currentRead;

                if (readCount > 0) {
                    return readCount;
                }
            } while (readToCache(currentLength));

            return readCount > 0 ? readCount : -1;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        @Override
        public void mark(int readlimit) {
        }

        @Override
        public void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }

        @Override
        public boolean markSupported() {
            return false;
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
