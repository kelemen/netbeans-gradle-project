package org.netbeans.gradle.project.view;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
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
            Lookup actionContext) {

        return createProjectTaskBuilderSimple(kind, command, config, project.getVarReplaceMap(actionContext));
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
            TaskKind kind, String command, NbGradleConfiguration config, TaskVariableMap varReplaceMap) {

        waitForProjectLoad();
        PredefinedTask task = getBuiltInTask(command, config);

        String caption = getOutputTabCaption(kind);
        return task.createTaskDefBuilder(caption, varReplaceMap);
    }

    private GradleTaskDef.Builder createProjectTaskBuilderMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config,
            Lookup actionContext) {

        GradleTaskDef.Builder builder = createProjectTaskBuilder(kind, command, config, actionContext);
        if (GlobalGradleSettings.getSkipTests().getValue()) {
            builder.addArguments(Arrays.asList("-x", "test"));
        }
        return builder;
    }

    private Runnable createProjectTask(
            final TaskKind kind,
            final String command,
            final NbGradleConfiguration config,
            final Lookup actionContext) {
        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilder(kind, command, config, actionContext).create();
            }
        }, projectTaskCompleteListener());
    }

    private Runnable createProjectTaskMaySkipTest(
            TaskKind kind,
            String command,
            NbGradleConfiguration config,
            Lookup actionContext) {
        return createProjectTaskMaySkipTest(kind, command, config, actionContext, projectTaskCompleteListener());
    }

    private Runnable createProjectTaskMaySkipTest(
            final TaskKind kind,
            final String command,
            final NbGradleConfiguration config,
            final Lookup actionContext,
            TaskCompleteListener listener) {

        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                return createProjectTaskBuilderMaySkipTest(kind, command, config, actionContext).create();
            }
        }, listener);
    }

    private Runnable createDebugTask(
            final String command,
            final boolean test,
            final NbGradleConfiguration config,
            final Lookup actionContext) {

        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                GradleTaskDef.Builder builder = createProjectTaskBuilderMaySkipTest(
                        TaskKind.DEBUG, command, config, actionContext);
                builder.setStdOutListener(debugeeListener(test));
                return builder.create();
            }
        }, projectTaskCompleteListener());
    }

    private Runnable createAction(String command, Lookup context) {
        NbGradleConfiguration config = context != null
                ? context.lookup(NbGradleConfiguration.class)
                : null;

        if (COMMAND_BUILD.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config, context);
        }
        else if (COMMAND_TEST.equals(command)) {
            return createProjectTask(TaskKind.BUILD, command, config, context);
        }
        else if (COMMAND_CLEAN.equals(command)) {
            return createProjectTask(TaskKind.BUILD, command, config, context);
        }
        else if (COMMAND_REBUILD.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config, context);
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
            return createProjectTaskMaySkipTest(TaskKind.RUN, command, config, context);
        }
        else if (COMMAND_DEBUG.equals(command)) {
            return createDebugTask(command, false, config, context);
        }
        else if (COMMAND_JAVADOC.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.BUILD, command, config, context);
        }
        else if (COMMAND_TEST_SINGLE.equals(command)) {
            return createProjectTask(TaskKind.BUILD, command, config, context);
        }
        else if (COMMAND_DEBUG_TEST_SINGLE.equals(command)) {
            return createDebugTask(command, true, config, context);
        }
        else if (COMMAND_RUN_SINGLE.equals(command)) {
            return createProjectTaskMaySkipTest(TaskKind.RUN, command, config, context);
        }
        else if (COMMAND_DEBUG_SINGLE.equals(command)) {
            return createDebugTask(command, true, config, context);
        }
        else if (COMMAND_DEBUG_FIX.equals(command)) {
            final String className = DebugUtils.getActiveClassName(project, context);
            if (className.isEmpty()) {
                return null;
            }

            return createProjectTaskMaySkipTest(TaskKind.APPLY_CHANGES, command, config, context, new TaskCompleteListener() {
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

    private TaskCompleteListener projectTaskCompleteListener() {
        return GradleTasks.projectTaskCompleteListener(project);
    }

    private enum TaskKind {
        DEBUG,
        RUN,
        BUILD,
        APPLY_CHANGES
    }
}
