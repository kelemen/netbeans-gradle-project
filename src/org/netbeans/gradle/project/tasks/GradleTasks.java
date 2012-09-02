package org.netbeans.gradle.project.tasks;

import java.io.File;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class GradleTasks {
    public static final RequestProcessor TASK_EXECUTOR
            = new RequestProcessor("Gradle-Task-Executor", 10, true);

    private static final Logger LOGGER = Logger.getLogger(GradleTasks.class.getName());
    private static final String[] NO_ARGS = new String[0];

    private static File getJavaHome() {
        Collection<FileObject> installFolders = JavaPlatform.getDefault().getInstallFolders();
        if (installFolders.size() != 1) {
            return null;
        }
        else {
            return FileUtil.toFile(installFolders.iterator().next());
        }
    }

    private static void doGradleTasks(NbGradleProject project, GradleTaskDef taskDef) {
        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbStrings.getExecuteTasksText());
        try {
            progress.start();
            doGradleTasksWithProgress(progress, project, taskDef);
        } finally {
            progress.finish();
        }
    }

    private static void doGradleTasksWithProgress(
            final ProgressHandle progress,
            NbGradleProject project,
            GradleTaskDef taskDef) {
        String printableName = taskDef.getTaskNames().size() == 1
                ? taskDef.getTaskNames().get(0)
                : taskDef.getTaskNames().toString();

        StringBuilder commandBuilder = new StringBuilder(128);
        commandBuilder.append("gradle");
        for (String task: taskDef.getTaskNames()) {
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
            buildLauncher.setJavaHome(getJavaHome());
            if (!taskDef.getJvmArguments().isEmpty()) {
                buildLauncher.setJvmArguments(taskDef.getJvmArgumentsArray());
            }
            if (!taskDef.getArguments().isEmpty()) {
                buildLauncher.withArguments(taskDef.getArgumentArray());
            }

            buildLauncher.addProgressListener(new ProgressListener() {
                @Override
                public void statusChanged(ProgressEvent pe) {
                    progress.progress(pe.getDescription());
                }
            });

           buildLauncher.forTasks(taskDef.getTaskNamesArray());

           InputOutput io = IOProvider.getDefault().getIO("Gradle: " + printableName, false);
           OutputWriter buildOutput = io.getOut();
           try {
               buildOutput.println(NbStrings.getExecutingTaskMessage(command));
               if (!taskDef.getArguments().isEmpty()) {
                   buildOutput.println(NbStrings.getTaskArgumentsMessage(taskDef.getArguments()));
               }
               if (!taskDef.getJvmArguments().isEmpty()) {
                   buildOutput.println(NbStrings.getTaskJvmArgumentsMessage(taskDef.getJvmArguments()));
               }

               buildOutput.println();

               OutputWriter buildErrOutput = io.getErr();
               try {
                   Writer forwardedStdOut = new WriterForwarded(buildOutput, taskDef.getStdOutListener());
                   Writer forwardedStdErr = new WriterForwarded(buildOutput, taskDef.getStdOutListener());

                   buildLauncher.setStandardOutput(new WriterOutputStream(forwardedStdOut));
                   buildLauncher.setStandardError(new WriterOutputStream(forwardedStdErr));

                   io.select();
                   buildLauncher.run();
               } catch (BuildException ex) {
                   // Gradle should have printed this one to stderr.
                   LOGGER.log(Level.INFO, "Gradle build failure: " + command, ex);
               } catch (Throwable ex) {
                   buildErrOutput.println();
                   ex.printStackTrace(buildErrOutput);
                   LOGGER.log(
                           ex instanceof Exception ? Level.INFO : Level.SEVERE,
                           "Gradle build failure: " + command,
                           ex);
               } finally {
                   buildErrOutput.close();
               }
           } finally {
               buildOutput.close();
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

    private static void submitGradleTask(final NbGradleProject project, GradleTaskDef taskDef) {
        preSubmitGradleTask();

        List<String> globalJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();

        final GradleTaskDef newTaskDef;
        if (globalJvmArgs != null && !globalJvmArgs.isEmpty()) {
            GradleTaskDef.Builder builder = new GradleTaskDef.Builder(taskDef);

            List<String> combinedJvmArgs = new ArrayList<String>(
                    taskDef.getJvmArguments().size() + globalJvmArgs.size());
            combinedJvmArgs.addAll(taskDef.getJvmArguments());
            combinedJvmArgs.addAll(globalJvmArgs);
            builder.setJvmArguments(combinedJvmArgs);

            newTaskDef = builder.create();
        }
        else {
            newTaskDef = taskDef;
        }

        TASK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                doGradleTasks(project, newTaskDef);
            }
        });
    }

    public static Runnable createAsyncGradleTask(NbGradleProject project, GradleTaskDef taskDef) {
        return new AsyncGradleTask(project, taskDef);
    }

    private static class AsyncGradleTask implements Runnable {
        private final NbGradleProject project;
        private final GradleTaskDef taskDef;

        public AsyncGradleTask(NbGradleProject project, GradleTaskDef taskDef) {
            if (project == null) throw new NullPointerException("project");
            if (taskDef == null) throw new NullPointerException("taskDef");

            this.project = project;
            this.taskDef = taskDef;
        }

        @Override
        public void run() {
            submitGradleTask(project, taskDef);
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

    private static class WriterForwarded extends Writer {
        private final Writer wrapped;
        private final TaskOutputListener listener;

        public WriterForwarded(Writer wrapped, TaskOutputListener listener) {
            if (wrapped == null) throw new NullPointerException("wrapped");
            if (listener == null) throw new NullPointerException("listener");

            this.wrapped = wrapped;
            this.listener = listener;
        }

        @Override
        public void write(int c) throws IOException {
            wrapped.write(c);
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            listener.receiveOutput(cbuf, 0, cbuf.length);
            wrapped.write(cbuf);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            listener.receiveOutput(cbuf, off, len);
            wrapped.write(cbuf, off, len);
        }

        @Override
        public void write(String str) throws IOException {
            char[] cbuf = str.toCharArray();
            listener.receiveOutput(cbuf, 0, cbuf.length);
            wrapped.write(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            char[] cbuf = str.toCharArray();
            listener.receiveOutput(cbuf, off, len);
            wrapped.write(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            char[] cbuf = csq.toString().toCharArray();
            listener.receiveOutput(cbuf, 0, cbuf.length);
            return wrapped.append(csq);
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            char[] cbuf = csq.toString().toCharArray();
            listener.receiveOutput(cbuf, start, end - start);
            return wrapped.append(csq, start, end);
        }

        @Override
        public Writer append(char c) throws IOException {
            listener.receiveOutput(new char[]{c}, 0, 1);
            return wrapped.append(c);
        }

        @Override
        public void flush() throws IOException {
            wrapped.flush();
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }

    private GradleTasks() {
        throw new AssertionError();
    }
}
