package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
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
import org.netbeans.gradle.project.model.GradleModelLoader;
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

    private static void doGradleTasks(
            NbGradleProject project,
            String[] taskNames,
            String[] arguments,
            String[] jvmArguments) {
        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbStrings.getExecuteTasksText());
        try {
            progress.start();
            doGradleTasksWithProgress(progress, project, taskNames, arguments, jvmArguments);
        } finally {
            progress.finish();
        }
    }

    private static void doGradleTasksWithProgress(
            final ProgressHandle progress,
            NbGradleProject project,
            String[] taskNames,
            String[] arguments,
            String[] jvmArguments) {
        if (taskNames.length < 1) {
            throw new IllegalArgumentException("At least one task is required.");
        }
        String printableName = taskNames.length == 1
                ? taskNames[0]
                : Arrays.toString(taskNames);

        StringBuilder commandBuilder = new StringBuilder(128);
        commandBuilder.append("gradle");
        for (String task: taskNames) {
            commandBuilder.append(' ');
            commandBuilder.append(task);
        }

        String command = commandBuilder.toString();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "Executing: {0}, Args = {1}, JvmArg = {2}",
                    new Object[]{command, Arrays.toString(arguments), Arrays.toString(jvmArguments)});
        }

        FileObject projectDir = project.getProjectDirectory();

        GradleConnector gradleConnector = GradleModelLoader.createGradleConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();
            BuildLauncher buildLauncher = projectConnection.newBuild();
            buildLauncher.setJavaHome(getJavaHome());
            if (jvmArguments.length > 0) {
                buildLauncher.setJvmArguments(jvmArguments);
            }
            if (arguments.length > 0) {
                buildLauncher.withArguments(arguments);
            }

            buildLauncher.addProgressListener(new ProgressListener() {
                @Override
                public void statusChanged(ProgressEvent pe) {
                    progress.progress(pe.getDescription());
                }
            });

           buildLauncher.forTasks(taskNames);

           InputOutput io = IOProvider.getDefault().getIO("Gradle: " + printableName, false);
           OutputWriter buildOutput = io.getOut();
           try {
               buildOutput.println(NbStrings.getExecutingTaskMessage(command));
               if (arguments.length > 0) {
                   buildOutput.println(NbStrings.getTaskArgumentsMessage(arguments));
               }
               if (jvmArguments.length > 0) {
                   buildOutput.println(NbStrings.getTaskJvmArgumentsMessage(arguments));
               }

               buildOutput.println();

               OutputWriter buildErrOutput = io.getErr();
               try {
                   buildLauncher.setStandardOutput(new WriterOutputStream(buildOutput));
                   buildLauncher.setStandardError(new WriterOutputStream(buildErrOutput));

                   io.select();
                   buildLauncher.run();
               } finally {
                   buildErrOutput.close();
               }
           } finally {
               buildOutput.close();
           }
        } catch (BuildException ex) {
            // Gradle should have printed this one to stderr.
            LOGGER.log(Level.WARNING, "Gradle build failure: " + command, ex);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }
    }

    private static void submitGradleTask(final NbGradleProject project,
            String[] taskNames,
            String[] arguments,
            String[] jvmArguments) {

        final String[] taskNamesCopy = taskNames.clone();
        final String[] argumentsCopy = arguments.clone();
        final String[] jvmArgumentsCopy = jvmArguments.clone();
        TASK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                doGradleTasks(project, taskNamesCopy, argumentsCopy, jvmArgumentsCopy);
            }
        });
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            String[] taskNames,
            String[] arguments,
            String[] jvmArguments) {
        return new AsyncGradleTask(project, taskNames, arguments, jvmArguments);
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            String[] taskNames,
            String[] arguments) {
        return createAsyncGradleTask(project, taskNames, arguments, NO_ARGS);
    }

    public static Runnable createAsyncGradleTask(
            NbGradleProject project,
            String... taskNames) {
        return createAsyncGradleTask(project, taskNames, NO_ARGS, NO_ARGS);
    }

    private static class AsyncGradleTask implements Runnable {
        private final NbGradleProject project;
        private final String[] taskNames;
        private final String[] arguments;
        private final String[] jvmArguments;

        public AsyncGradleTask(
                NbGradleProject project,
                String[] taskNames,
                String[] arguments,
                String[] jvmArguments) {
            if (project == null) throw new NullPointerException("project");
            if (taskNames == null) throw new NullPointerException("taskNames");
            if (arguments == null) throw new NullPointerException("arguments");
            if (jvmArguments == null) throw new NullPointerException("jvmArguments");

            this.project = project;
            this.taskNames = taskNames.clone();
            this.arguments = arguments.clone();
            this.jvmArguments = jvmArguments.clone();
        }

        @Override
        public void run() {
            submitGradleTask(project, taskNames, arguments, jvmArguments);
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

    private GradleTasks() {
        throw new AssertionError();
    }
}
