package org.netbeans.gradle.project.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.output.InputOutputManager;
import org.netbeans.gradle.project.output.SmartOutputHandler;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.MutableProperty;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.PropertiesLoadListener;
import org.netbeans.gradle.project.tasks.AttacherListener;
import org.netbeans.gradle.project.tasks.BuiltInTasks;
import org.netbeans.gradle.project.tasks.DebugUtils;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.gradle.project.tasks.TaskCompleteListener;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

import static org.netbeans.api.java.project.JavaProjectConstants.COMMAND_DEBUG_FIX;
import static org.netbeans.api.java.project.JavaProjectConstants.COMMAND_JAVADOC;
import static org.netbeans.spi.project.ActionProvider.*;

public class GradleActionProvider implements ActionProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleActionProvider.class.getName());

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
        COMMAND_DEBUG_TEST_SINGLE,
        COMMAND_RUN_SINGLE,
        COMMAND_DEBUG_SINGLE,
        COMMAND_DEBUG_FIX
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

    protected List<FileObject> getFilesOfContext(Lookup context) {
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

        final ProjectProperties properties = getLoadedProperties(config);

        MutableProperty<PredefinedTask> taskProperty = properties.tryGetBuiltInTask(command);
        PredefinedTask result = taskProperty != null
                ? taskProperty.getValue()
                : null;

        if (result == null) {
            result = BuiltInTasks.getDefaultBuiltInTask(command);
        }
        return result;
    }

    private GradleTaskDef.Builder createProjectTaskBuilder(
            TaskKind kind,
            String command,
            NbGradleConfiguration config,
            Map<String, String> additionalVarReplaces) {

        Map<String, String> varReplaceMap = PredefinedTask.varReplaceMap(project.getAvailableModel().getMainModule());
        if (!additionalVarReplaces.isEmpty()) {
            varReplaceMap = new HashMap<String, String>(varReplaceMap);
            varReplaceMap.putAll(additionalVarReplaces);
        }
        return createProjectTaskBuilderSimple(kind, command, config, varReplaceMap);
    }

    private String getOutputTabCaption(TaskKind kind) {
        switch (kind) {
            case DEBUG:
                return project.getDisplayName() + " - debug";
            case RUN:
                return project.getDisplayName() + " - run";
            case BUILD:
                return project.getDisplayName();
            case APPLY_CHANGES:
                return project.getDisplayName() + " - fix";
            default:
                throw new AssertionError(kind.name());
        }
    }

    private GradleTaskDef.Builder createProjectTaskBuilderSimple(
            TaskKind kind, String command, NbGradleConfiguration config, Map<String, String> varReplaceMap) {

        waitForProjectLoad();
        PredefinedTask task = getBuiltInTask(command, config);

        String caption = getOutputTabCaption(kind);
        return task.createTaskDefBuilder(caption, varReplaceMap);
    }

    private GradleTaskDef.Builder createProjectTaskBuilderMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config) {
        return createProjectTaskBuilderMaySkipTest(
                kind,
                command,
                config,
                Collections.<String, String>emptyMap());
    }

    private GradleTaskDef.Builder createProjectTaskBuilderMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config,
            Map<String, String> additionalVarReplaces) {

        GradleTaskDef.Builder builder = createProjectTaskBuilder(kind, command, config, additionalVarReplaces);
        if (GlobalGradleSettings.getSkipTests().getValue()) {
            builder.addArguments(Arrays.asList("-x", "test"));
        }
        return builder;
    }
    private Runnable createProjectTask(
            final TaskKind kind,
            final String command,
            final NbGradleConfiguration config) {
        return createProjectTask(kind, command, config, Collections.<String, String>emptyMap());
    }

    private Runnable createProjectTask(
            final TaskKind kind,
            final String command,
            final NbGradleConfiguration config,
            Map<String, String> additionalVarReplaces) {

        final Map<String, String> copyVars = new HashMap<String, String>(additionalVarReplaces);
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilder(kind, command, config, copyVars).create();
            }
        }, projectTaskCompleteListener());
    }

    private Runnable createProjectTaskMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config) {
        return createProjectTaskMaySkipTest(kind, command, config, projectTaskCompleteListener());
    }

    private Runnable createProjectTaskMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config,
            TaskCompleteListener listener) {
        return createProjectTaskMaySkipTest(
                kind,
                command,
                config,
                Collections.<String, String>emptyMap(),
                listener);
    }

    private Runnable createProjectTaskMaySkipTest(
            final TaskKind kind,
            final String command,
            final NbGradleConfiguration config,
            Map<String, String> additionalVarReplaces,
            TaskCompleteListener listener) {

        final Map<String, String> copyVars = new HashMap<String, String>(additionalVarReplaces);
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilderMaySkipTest(kind, command, config, copyVars).create();
            }
        }, listener);
    }

    private Runnable createDebugTask(
            final String command,
            final boolean test,
            final NbGradleConfiguration config) {

        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                GradleTaskDef.Builder builder = createProjectTaskBuilderMaySkipTest(
                        TaskKind.DEBUG, command, config);
                builder.setStdOutListener(debugeeListener(test));
                return builder.create();
            }
        }, projectTaskCompleteListener());
    }

    protected FileObject getJavaFileOfContext(Lookup context) {
        List<FileObject> files = getFilesOfContext(context);
        if (files.isEmpty()) {
            return null;
        }

        FileObject file = files.get(0);
        if (file == null) {
            return null;
        }

        String fileExt = file.getExt().toLowerCase(Locale.US);
        if (!"java".equals(fileExt)
                && !"groovy".equals(fileExt)) {
            return null;
        }

        return file;
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
            FileObject file = getJavaFileOfContext(context);

            return file != null
                    ? new TestSingleTask(file, command, COMMAND_DEBUG_TEST_SINGLE.equals(command), config)
                    : null;
        }
        else if (COMMAND_RUN_SINGLE.equals(command)) {
            FileObject file = getJavaFileOfContext(context);
            return file != null
                    ? new ExecuteSingleTask(file, command, TaskKind.RUN, config)
                    : null;
        }
        else if (COMMAND_DEBUG_SINGLE.equals(command)) {
            FileObject file = getJavaFileOfContext(context);
            return file != null
                    ? new ExecuteSingleTask(file, command, TaskKind.DEBUG, config)
                    : null;
        }
        else if (COMMAND_DEBUG_FIX.equals(command)) {
            final String className = DebugUtils.getActiveClassName(project, context);
            if (className.isEmpty()) {
                return null;
            }

            return createProjectTaskMaySkipTest(TaskKind.APPLY_CHANGES, command, config, new TaskCompleteListener() {
                @Override
                public void onComplete(Throwable error) {
                    if (error == null) {
                        InputOutputManager.IORef ref = InputOutputManager.getInputOutput(
                                getOutputTabCaption(TaskKind.APPLY_CHANGES), true, false);
                        try {
                            DebugUtils.applyChanges(project, ref.getIo().getOut(), className);
                        } finally {
                            try {
                                ref.close();
                            } catch (Throwable ex) {
                                LOGGER.log(Level.SEVERE, "Failed to close IORef.", ex);
                            }
                        }
                    }
                    else {
                        projectTaskCompleteListener().onComplete(error);
                    }
                }
            });
        }

        return null;
    }

    private static String removeExtension(String filePath) {
        int extSeparatorIndex = filePath.lastIndexOf('.');
        return extSeparatorIndex >= 0
                ? filePath.substring(0, extSeparatorIndex)
                : filePath;
    }

    private FileContext getFileContext(FileObject file) {
        NbGradleModule mainModule = project.getAvailableModel().getMainModule();

        NbSourceType type = null;
        String testFileName = null;

        for (Map.Entry<NbSourceType, NbSourceGroup> group: mainModule.getSources().entrySet()) {
            for (FileObject sourceFile: group.getValue().getFileObjects()) {
                String relPath = FileUtil.getRelativePath(sourceFile, file);
                if (relPath != null) {
                    // Remove the ".java" from the end of
                    // the file name
                    testFileName = removeExtension(relPath);
                    type = group.getKey();
                    break;
                }
            }
        }

        if (type != null && testFileName != null) {
            Map<String, String> result = new HashMap<String, String>();
            result.put(PredefinedTask.VAR_TEST_FILE_PATH, testFileName);
            result.put(PredefinedTask.VAR_SELECTED_CLASS, testFileName.replace('/', '.'));
            return new FileContext(type, result);
        }
        else {
            return null;
        }
    }

    private TaskCompleteListener projectTaskCompleteListener() {
        return GradleTasks.projectTaskCompleteListener(project);
    }

    private static final class FileContext {
        private final NbSourceType sourceType;
        private final Map<String, String> varReplaceMap;

        public FileContext(NbSourceType sourceType, Map<String, String> varReplaceMap) {
            assert sourceType != null;
            assert varReplaceMap != null;

            this.sourceType = sourceType;
            this.varReplaceMap = Collections.unmodifiableMap(new HashMap<String, String>(varReplaceMap));
        }

        public NbSourceType getSourceType() {
            return sourceType;
        }

        public Map<String, String> getVarReplaceMap() {
            return varReplaceMap;
        }
    }

    private abstract class AbstractFileTask implements Runnable {
        private final FileObject file;
        private final TaskKind taskKind;
        private final String command;
        private final NbGradleConfiguration config;

        public AbstractFileTask(FileObject file, String command, TaskKind taskKind, NbGradleConfiguration config) {
            this.file = file;
            this.taskKind = taskKind;
            this.command = command;
            this.config = config;
        }

        protected abstract boolean needTestClasses(FileContext fileContext);

        @Override
        public final void run() {
            Runnable testTask = GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
                @Override
                public GradleTaskDef call() {
                    FileContext fileContext = getFileContext(file);

                    if (fileContext != null) {
                        GradleTaskDef.Builder builder = createProjectTaskBuilder(
                                taskKind, command, config, fileContext.getVarReplaceMap());

                        if (taskKind == TaskKind.DEBUG) {
                            builder.setStdOutListener(debugeeListener(true));
                        }
                        return builder.create();
                    }
                    else {
                        LOGGER.log(Level.WARNING, "Failed to find test file to execute: {0}", file);
                        return null;
                    }
                }
            }, projectTaskCompleteListener());
            testTask.run();
        }
    }

    private class ExecuteSingleTask extends AbstractFileTask {
        public ExecuteSingleTask(FileObject file, String command, TaskKind taskKind, NbGradleConfiguration config) {
            super(file, command, taskKind, config);
        }

        @Override
        protected boolean needTestClasses(FileContext fileContext) {
            return fileContext.getSourceType().isTest();
        }
    }

    private class TestSingleTask extends AbstractFileTask {
        public TestSingleTask(FileObject file, String command, boolean debug, NbGradleConfiguration config) {
            super(file, command, debug ? TaskKind.DEBUG : TaskKind.BUILD, config);
        }

        @Override
        protected boolean needTestClasses(FileContext fileContext) {
            return true;
        }
    }

    private enum TaskKind {
        DEBUG,
        RUN,
        BUILD,
        APPLY_CHANGES
    }
}
