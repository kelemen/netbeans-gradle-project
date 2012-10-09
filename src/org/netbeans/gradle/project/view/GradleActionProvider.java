package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.DebugTextListener;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTaskDef.Builder;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.gradle.project.tasks.TaskOutputListener;
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

    private TaskOutputListener debugeeListener(boolean test) {
        return new DebugTextListener(new AttacherListener(project, test));
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

    private static List<String> toQualifiedTaskName(NbGradleProject project, String... tasks) {
        String qualifier = project.getAvailableModel().getMainModule().getUniqueName() + ":";

        List<String> qualified = new ArrayList<String>(tasks.length);
        for (String task: tasks) {
            qualified.add(qualifier + task);
        }
        return qualified;
    }

    private GradleTaskDef.Builder createProjectTaskBuilder(String... tasks) {
        List<String> qualified = toQualifiedTaskName(project, tasks);
        return new GradleTaskDef.Builder(qualified);
    }

    private GradleTaskDef.Builder createProjectTaskBuilder(
            boolean addSkipTestIfNeeded,
            boolean nonBlocking,
            String... tasks) {

        GradleTaskDef.Builder builder = createProjectTaskBuilder(tasks);
        if (addSkipTestIfNeeded && GlobalGradleSettings.getSkipTests().getValue()) {
            builder.setArguments(Arrays.asList("-x", "test"));
        }
        builder.setNonBlocking(nonBlocking);
        return builder;
    }

    private Runnable createProjectTask(
            boolean skipTestIfNeeded,
            boolean nonBlocking,
            String... tasks) {
        Builder builder = createProjectTaskBuilder(skipTestIfNeeded, nonBlocking, tasks);
        return GradleTasks.createAsyncGradleTask(project, builder.create());
    }

    private Runnable createDebugTask(boolean test, String... tasks) {
        Builder builder = createProjectTaskBuilder(false, false, tasks);
        builder.setStdOutListener(debugeeListener(test));
        return GradleTasks.createAsyncGradleTask(project, builder.create());
    }

    private Runnable createAction(String command, Lookup context) {
        if (COMMAND_BUILD.equals(command)) {
            return createProjectTask(true, true, "build");
        }
        else if (COMMAND_TEST.equals(command)) {
            return createProjectTask(false, true, "cleanTest", "test");
        }
        else if (COMMAND_CLEAN.equals(command)) {
            return createProjectTask(false, true, "clean");
        }
        else if (COMMAND_REBUILD.equals(command)) {
            return createProjectTask(true, true, "clean", "build");
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
            return createProjectTask(true, false, "run");
        }
        else if (COMMAND_DEBUG.equals(command)) {
            return createDebugTask(false, "debug");
        }
        else if (COMMAND_JAVADOC.equals(command)) {
            return createProjectTask(false, true, "javadoc");
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
            Runnable testTask = GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
                @Override
                public GradleTaskDef call() {
                    NbGradleModule mainModule = project.getAvailableModel().getMainModule();

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

                        List<String> qualifiedTaskNames
                                = toQualifiedTaskName(project, "cleanTest", "test");

                        GradleTaskDef.Builder builder = new GradleTaskDef.Builder(qualifiedTaskNames);
                        builder.setArguments(Arrays.asList(args));
                        if (debug) {
                            builder.setStdOutListener(debugeeListener(true));
                        }

                        return builder.create();
                    }
                    else {
                        LOGGER.log(Level.WARNING, "Failed to find test file to execute: {0}", file);
                        return null;
                    }
                }
            });
            testTask.run();
        }
    }
}
