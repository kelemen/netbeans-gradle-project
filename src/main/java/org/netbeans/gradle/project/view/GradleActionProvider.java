package org.netbeans.gradle.project.view;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.WaitableSignal;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.properties.MutableProperty;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.PropertiesLoadListener;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;


public final class GradleActionProvider implements ActionProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleActionProvider.class.getName());

    public static final String COMMAND_RELOAD = "reload";

    private final NbGradleProject project;

    public GradleActionProvider(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public String[] getSupportedActions() {
        return project.getMergedCommandQuery().getSupportedCommands().toArray(new String[0]);
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
            properties = project.getPropertiesForProfile(config.getProfileDef(), true, new PropertiesLoadListener() {
                @Override
                public void loadedProperties(ProjectProperties properties) {
                    loadedSignal.signal();
                }
            });
            loadedSignal.tryWaitForSignal();
        }
        return properties;
    }

    private CustomCommandActions getCommandActions(NbGradleConfiguration config, String command) {
        ProfileDef profileDef = config.getProfileDef();
        return project.getMergedCommandQuery().tryGetCommandDefs(profileDef, command);
    }

    private Runnable createAction(final String command, Lookup context) {
        if (command == null) {
            return null;
        }

        if (COMMAND_RELOAD.equals(command)) {
            return new Runnable() {
                @Override
                public void run() {
                    project.reloadProject();
                }
            };
        }

        final Lookup appliedContext = context != null ? context : Lookup.EMPTY;

        NbGradleConfiguration config = appliedContext.lookup(NbGradleConfiguration.class);
        final NbGradleConfiguration appliedConfig = config != null
                ? config
                : project.getCurrentProfile();

        final AtomicReference<CustomCommandActions> customActionsRef
                = new AtomicReference<CustomCommandActions>(null);

        return GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
            @Override
            public GradleTaskDef call() {
                ProjectProperties properties = getLoadedProperties(appliedConfig);
                MutableProperty<PredefinedTask> builtInTask = properties.tryGetBuiltInTask(command);
                if (builtInTask == null) {
                    return null;
                }

                final PredefinedTask task = builtInTask.getValue();
                if (task == null) {
                    LOGGER.log(Level.WARNING, "Property returns null for built-in command: {0}", command);
                    return null;
                }

                CustomCommandActions customActions = getCommandActions(appliedConfig, command);
                if (customActions == null) {
                    customActions = CustomCommandActions.OTHER;
                }
                customActionsRef.set(customActions);

                return GradleTaskDef.createFromTemplate(project,
                        task.toCommandTemplate(),
                        customActions,
                        appliedContext).create();
            }
        }, new CommandCompleteListener() {
            @Override
            public void onComplete(Throwable error) {
                try {
                    CustomCommandActions customActions = customActionsRef.get();
                    if (customActions != null) {
                        CommandCompleteListener completeListener
                                = customActions.getCommandCompleteListener();
                        if (completeListener != null) {
                            completeListener.onComplete(error);
                        }
                    }
                } finally {
                    GradleTasks.projectTaskCompleteListener(project).onComplete(error);
                }
            }
        });
    }
}
