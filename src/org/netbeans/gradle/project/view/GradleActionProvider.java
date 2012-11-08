package org.netbeans.gradle.project.view;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.MutableProperty;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.PropertiesLoadListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.BuiltInTasks;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTasks;
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

    private SmartOutputHandler.Visitor debugeeListener(boolean test) {
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

    private void checkNotEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Cannot be called from the EDT.");
        }
    }

    private void waitForProjectLoad() {
        checkNotEdt();
        project.tryWaitForLoadedProject();
    }

    private ProjectProperties getLoadedProperties(NbGradleConfiguration config) {
        checkNotEdt();

        ProjectProperties properties;
        if (config == null) {
            properties = project.tryGetLoadedProperties();
            if (properties == null) {
                properties = project.getProperties();
            }
        }
        else {
            final WaitableSignal loadedSignal = new WaitableSignal();
            properties = project.getPropertiesForProfile(config.getProfileName(), true, new PropertiesLoadListener() {
                @Override
                public void loadedProperties(ProjectProperties properties) {
                    loadedSignal.signal();
                }
            });
            loadedSignal.tryWaitForSignal();
        }
        return properties;
    }

    private PredefinedTask getBuiltInTask(
            final String command,
            NbGradleConfiguration config) {
        assert command != null;

        checkNotEdt();

        final ProjectProperties properties = getLoadedProperties(config);
        final AtomicReference<PredefinedTask> resultRef = new AtomicReference<PredefinedTask>(null);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    MutableProperty<PredefinedTask> task = properties.tryGetBuiltInTask(command);
                    if (task != null) {
                        resultRef.set(task.getValue());
                    }
                }
            });
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        PredefinedTask result = resultRef.get();
        if (result == null) {
            result = BuiltInTasks.getDefaultBuiltInTask(command);
        }
        return result;
    }

    private GradleTaskDef.Builder createProjectTaskBuilder(TaskKind kind, String command, NbGradleConfiguration config) {
        Map<String, String> varReplaceMap = PredefinedTask.varReplaceMap(project.getAvailableModel().getMainModule());
        return createProjectTaskBuilder(kind, command, config, varReplaceMap);
    }

    private GradleTaskDef.Builder createProjectTaskBuilder(
            TaskKind kind, String command, NbGradleConfiguration config, Map<String, String> varReplaceMap) {

        waitForProjectLoad();
        PredefinedTask task = getBuiltInTask(command, config);

        String caption;
        switch (kind) {
            case DEBUG:
                caption = project.getDisplayName() + " - debug";
                break;
            case RUN:
                caption = project.getDisplayName() + " - run";
                break;
            case BUILD:
                caption = project.getDisplayName();
                break;
            default:
                throw new AssertionError(kind.name());
        }

        return task.createTaskDefBuilder(caption, varReplaceMap);
    }

    private GradleTaskDef.Builder createProjectTaskBuilderMaySkipTest(TaskKind kind, String command, NbGradleConfiguration config) {
        GradleTaskDef.Builder builder = createProjectTaskBuilder(kind, command, config);
        if (GlobalGradleSettings.getSkipTests().getValue()) {
            builder.setArguments(Arrays.asList("-x", "test"));
        }
        return builder;
    }

    private Runnable createProjectTask(final TaskKind kind, final String command, final NbGradleConfiguration config) {
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilder(kind, command, config).create();
            }
        });
    }

    private Runnable createProjectTaskMaySkipTest(final TaskKind kind, final String command, final NbGradleConfiguration config) {
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilderMaySkipTest(kind, command, config).create();
            }
        });
    }

    private Runnable createDebugTask(final String command, final boolean test, final NbGradleConfiguration config) {
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                GradleTaskDef.Builder builder = createProjectTaskBuilderMaySkipTest(TaskKind.DEBUG, command, config);
                builder.setStdOutListener(debugeeListener(test));
                return builder.create();
            }
        });
    }

    private Runnable createAction(String command, Lookup context) {
        NbGradleConfiguration config = context != null
                ? context.lookup(NbGradleConfiguration.class)
                : null;

        if (COMMAND_BUILD.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config);
        }
        else if (COMMAND_TEST.equals(command)) {
            return createProjectTask(TaskKind.BUILD, command, config);
        }
        else if (COMMAND_CLEAN.equals(command)) {
            return createProjectTask(TaskKind.BUILD, command, config);
        }
        else if (COMMAND_REBUILD.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config);
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
            return createProjectTaskMaySkipTest(TaskKind.RUN, command, config);
        }
        else if (COMMAND_DEBUG.equals(command)) {
            return createDebugTask(command, false, config);
        }
        else if (COMMAND_JAVADOC.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config);
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

            return new TestSingleTask(file, command, COMMAND_DEBUG_TEST_SINGLE.equals(command), config);
        }

        return null;
    }

    private class TestSingleTask implements Runnable {
        private final FileObject file;
        private final boolean debug;
        private final String command;
        private final NbGradleConfiguration config;

        public TestSingleTask(FileObject file, String command, boolean debug, NbGradleConfiguration config) {
            this.file = file;
            this.debug = debug;
            this.command = command;
            this.config = config;
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
                        Map<String, String> varReplaceMap = new HashMap<String, String>();
                        varReplaceMap.putAll(PredefinedTask.varReplaceMap(mainModule));
                        varReplaceMap.put(PredefinedTask.VAR_TEST_FILE_PATH, testFileName);

                        TaskKind kind = debug ? TaskKind.DEBUG : TaskKind.BUILD;
                        GradleTaskDef.Builder builder = createProjectTaskBuilder(kind, command, config, varReplaceMap);
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

    private enum TaskKind {
        DEBUG,
        RUN,
        BUILD
    }
}
