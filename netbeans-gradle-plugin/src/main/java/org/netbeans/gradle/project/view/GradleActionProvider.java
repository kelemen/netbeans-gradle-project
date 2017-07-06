package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationToken;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleActionProviderContext;
import org.netbeans.gradle.project.api.task.NbCommandString;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.standard.BuiltInTasks;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTaskDefFactory;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class GradleActionProvider implements ActionProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleActionProvider.class.getName());

    public static final String COMMAND_RELOAD = "reload";
    public static final String COMMAND_SET_AS_MAIN_PROJECT = "setAsMain";

    private final NbGradleProject project;

    public GradleActionProvider(NbGradleProject project) {
        this.project = project;
    }

    public static String getCommandStr(Lookup context, String defaultCommandStr) {
        NbCommandString commandStr = context.lookup(NbCommandString.class);
        return commandStr != null ? commandStr.getCommandString() : defaultCommandStr;
    }

    @Override
    public String[] getSupportedActions() {
        String[] actions = project.getMergedCommandQuery().getSupportedCommands().toArray(new String[0]);
        String[] result = new String[actions.length + 2];
        System.arraycopy(actions, 0, result, 0, actions.length);
        result[actions.length + 0] = COMMAND_RELOAD;
        result[actions.length + 1] = COMMAND_SET_AS_MAIN_PROJECT;
        return result;
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
        List<FileObject> files = new ArrayList<>();
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

    private NbGradleCommonProperties getLoadedCommonProperties(CancellationToken cancelToken, NbGradleConfiguration config) {
        checkNotEdt();

        NbGradleCommonProperties result = config != null
                    ? project.getProfileLoader().loadCommonPropertiesForProfile(config.getProfileKey())
                    : project.getCommonProperties();
        return result;
    }

    private CustomCommandActions getCommandActions(NbGradleConfiguration config, String command) {
        ProfileDef profileDef = config.getProfileDef();
        return project.getMergedCommandQuery().tryGetCommandDefs(profileDef, command);
    }

    private static String[] safeGetSupportedActions(ActionProvider provider) {
        String[] result = provider.getSupportedActions();
        return result != null ? result : new String[0];
    }

    private static boolean supportsAction(ActionProvider provider, String command) {
        for (String action: safeGetSupportedActions(provider)) {
            if (command.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryInvokeAction(Project project, String command, Lookup context) {
        Lookup projectLookup = project.getLookup();
        for (ActionProvider actionProvider: projectLookup.lookupAll(ActionProvider.class)) {
            if (supportsAction(actionProvider, command)) {
                actionProvider.invokeAction(command, context);
                return true;
            }
        }
        return false;
    }

    public static void invokeAction(Project project, String command, Lookup context) {
        if (!tryInvokeAction(project, command, context)) {
            LOGGER.log(Level.WARNING,
                    "Could not invoke command {0} for project {1}",
                    new Object[]{command, project.getProjectDirectory()});
        }
    }

    private static Lookup commandStringLookup(String command) {
        return Lookups.singleton(new NbCommandString(command));
    }

    private Lookup getAppliedContext(String command, Lookup context) {
        if (context == null) {
            return commandStringLookup(command);
        }

        NbCommandString currentNbCommandStr = context.lookup(NbCommandString.class);
        if (currentNbCommandStr == null) {
            return new ProxyLookup(commandStringLookup(command), context);
        }

        NbCommandString nbCommandString = new NbCommandString(command);
        if (nbCommandString.equals(currentNbCommandStr)) {
            return context;
        }
        return new ProxyLookup(Lookups.singleton(nbCommandString), context);
    }

    private String tryGetDisplayNameOfCommand(String command) {
        return project.getMergedCommandQuery().tryGetDisplayNameOfCommand(command);
    }

    private String getDisplayNameOfCommand(String command) {
        String displayName = tryGetDisplayNameOfCommand(command);
        if (displayName == null) {
            displayName = command;
        }
        return displayName;
    }

    private Runnable createAction(final String command, Lookup context) {
        if (command == null) {
            return null;
        }

        switch (command) {
            case COMMAND_RELOAD:
                return project::reloadProject;
            case COMMAND_SET_AS_MAIN_PROJECT:
                return () -> OpenProjects.getDefault().setMainProject(project);
        }

        final Lookup appliedContext = getAppliedContext(command, context);

        NbGradleConfiguration config = appliedContext.lookup(NbGradleConfiguration.class);
        final NbGradleConfiguration appliedConfig = config != null
                ? config
                : project.getConfigProvider().getActiveConfiguration();

        final AtomicReference<CustomCommandActions> customActionsRef
                = new AtomicReference<>(null);

        GradleTaskDefFactory taskDefFactory = new GradleTaskDefFactory() {
            private final AtomicReference<String> displayNameRef = new AtomicReference<>(null);

            @Override
            public String getDisplayName() {
                String result = displayNameRef.get();
                if (result == null) {
                    displayNameRef.compareAndSet(null, getDisplayNameOfCommand(command));
                    result = displayNameRef.get();
                }
                return result;
            }

            @Override
            public GradleTaskDef tryCreateTaskDef(CancellationToken cancelToken) throws Exception {
                NbGradleCommonProperties properties = getLoadedCommonProperties(cancelToken, appliedConfig);

                BuiltInTasks builtInTasks = properties.builtInTasks().getActiveValue();
                if (builtInTasks == null) {
                    LOGGER.log(Level.SEVERE, "Internal error: BuiltInTasks property is null.");
                    return null;
                }

                final PredefinedTask task = builtInTasks.tryGetByCommand(command);
                if (task == null) {
                    LOGGER.log(Level.WARNING, "Property returns null for built-in command: {0}", command);
                    return null;
                }

                CustomCommandActions customActions = getCommandActions(appliedConfig, command);
                if (customActions == null) {
                    customActions = CustomCommandActions.OTHER;
                }

                customActionsRef.set(customActions);

                String displayName = tryGetDisplayNameOfCommand(command);
                if (displayName == null) {
                    displayName = task.getDisplayName();
                }

                return GradleTaskDef.createFromTemplate(project,
                        task.toCommandTemplate(displayName),
                        customActions,
                        appliedContext).create();
            }
        };

        Set<GradleActionProviderContext> actionContexts = EnumSet.noneOf(GradleActionProviderContext.class);
        actionContexts.addAll(appliedContext.lookupAll(GradleActionProviderContext.class));

        return GradleTasks.createAsyncGradleTask(project, taskDefFactory, actionContexts, (Throwable error) -> {
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
        });
    }
}
