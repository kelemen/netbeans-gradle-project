package org.netbeans.gradle.project.view;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.GradleTasks;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

import static org.netbeans.spi.project.ActionProvider.*;

public final class GradleActionProvider implements ActionProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleActionProvider.class.getName());

    public static final String COMMAND_JAVADOC = "javadoc";
    public static final String COMMAND_RELOAD = "reload";

    private static final String[] SUPPORTED_ACTIONS = new String[]{
        COMMAND_BUILD,
        COMMAND_TEST,
        COMMAND_CLEAN,
        COMMAND_RUN,
        COMMAND_DEBUG,
        COMMAND_JAVADOC,
        COMMAND_REBUILD,
        COMMAND_RELOAD,
        COMMAND_TEST_SINGLE,
        COMMAND_DEBUG_TEST_SINGLE
    };

    private final NbGradleProject project;

    public GradleActionProvider(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public String[] getSupportedActions() {
        return SUPPORTED_ACTIONS.clone();
    }

    @Override
    public void invokeAction(String command, Lookup context) {
        Runnable task = createAction(command, context);
        if (task != null) {
            task.run();
        }
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) {
        return createAction(command, context) != null;
    }

    private static List<FileObject> getFilesOfContext(Lookup context) {
        List<FileObject> files = new LinkedList<FileObject>();
        for (DataObject dataObj: context.lookupAll(DataObject.class)) {
            FileObject file = dataObj.getPrimaryFile();
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    private static String[] toQualifiedTaskName(NbGradleProject project, String... tasks) {
        String qualifier = project.getCurrentModel().getMainModule().getUniqueName() + ":";

        String[] qualified = new String[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            qualified[i] = qualifier + tasks[i];
        }
        return qualified;
    }

    private static Runnable createProjectTask(NbGradleProject project, String... tasks) {
        String[] qualified = toQualifiedTaskName(project, tasks);
        return GradleTasks.createAsyncGradleTask(project, qualified);
    }

    private Runnable createAction(String command, Lookup context) {
        if (COMMAND_BUILD.equals(command)) {
            return createProjectTask(project, "build");
        }
        else if (COMMAND_TEST.equals(command)) {
            return createProjectTask(project, "cleanTest", "test");
        }
        else if (COMMAND_CLEAN.equals(command)) {
            return createProjectTask(project, "clean");
        }
        else if (COMMAND_REBUILD.equals(command)) {
            return createProjectTask(project, "clean", "build");
        }
        else if (COMMAND_RELOAD.equals(command)) {
            return new Runnable() {
                @Override
                public void run() {
                    project.reloadProject();
                }
            };
        }
        else if (COMMAND_RUN.equals(command)) {
            return createProjectTask(project, "run");
        }
        else if (COMMAND_DEBUG.equals(command)) {
            return createProjectTask(project, "debug");
        }
        else if (COMMAND_JAVADOC.equals(command)) {
            return createProjectTask(project, "javadoc");
        }
        else if (COMMAND_TEST_SINGLE.equals(command) || COMMAND_DEBUG_TEST_SINGLE.equals(command)) {
            List<FileObject> files = getFilesOfContext(context);
            if (files.isEmpty()) {
                return null;
            }
            final FileObject file = files.get(0);
            if (!"java".equals(file.getExt().toLowerCase(Locale.US))) {
                return null;
            }

            return new TestSingleTask(file, COMMAND_DEBUG_TEST_SINGLE.equals(command));
        }

        return null;
    }

    private class TestSingleTask implements Runnable {
        private final FileObject file;
        private final boolean debug;

        public TestSingleTask(FileObject file, boolean debug) {
            this.file = file;
            this.debug = debug;
        }

        @Override
        public void run() {
            NbGradleProject.TASK_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    NbGradleModule mainModule = project.getCurrentModel().getMainModule();

                    List<FileObject> sources = new LinkedList<FileObject>();
                    sources.addAll(mainModule.getSources(NbSourceType.SOURCE).getFileObjects());
                    sources.addAll(mainModule.getSources(NbSourceType.TEST_SOURCE).getFileObjects());

                    String testFileName = null;
                    for (FileObject sourceFile: sources) {
                        String relPath = FileUtil.getRelativePath(sourceFile, file);
                        if (relPath != null) {
                            // Remove the ".java" from the end of
                            // the file name
                            testFileName = relPath.substring(0, relPath.length() - 5);
                            break;
                        }
                    }

                    if (testFileName != null) {
                        String testArg = "-Dtest.single=" + testFileName;
                        String[] args = debug
                                ? new String[]{testArg, "-Dtest.debug"}
                                : new String[]{testArg};

                        Runnable task = GradleTasks.createAsyncGradleTask(
                                project,
                                toQualifiedTaskName(project, "cleanTest", "test"),
                                args);
                        task.run();
                    }
                    else {
                        LOGGER.log(Level.WARNING, "Failed to find test file to execute: {0}", file);
                    }
                }
            });
        }
    }
}
