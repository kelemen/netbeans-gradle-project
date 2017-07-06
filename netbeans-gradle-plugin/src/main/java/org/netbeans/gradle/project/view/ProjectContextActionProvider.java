package org.netbeans.gradle.project.view;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.AutoDisplayState;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;
import org.netbeans.gradle.project.properties.ui.AddNewTaskPanel;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.implspi.NamedServicesProvider;

public final class ProjectContextActionProvider implements ContextActionProvider {
    private final NbGradleProject project;

    public ProjectContextActionProvider(NbGradleProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    @Override
    public Action[] getActions() {
        TasksActionMenu tasksAction = new TasksActionMenu(project);
        CustomTasksActionMenu customTasksAction = new CustomTasksActionMenu(project);

        List<Action> projectActions = new ArrayList<>(32);
        projectActions.add(CommonProjectActions.newFileAction());
        projectActions.add(null);
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_RUN,
                NbStrings.getRunCommandCaption(true)));
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_DEBUG,
                NbStrings.getDebugCommandCaption(true)));
        projectActions.add(null);
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_BUILD,
                NbStrings.getBuildCommandCaption(true)));
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_TEST,
                NbStrings.getTestCommandCaption(true)));
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_CLEAN,
                NbStrings.getCleanCommandCaption(true)));
        projectActions.add(createProjectAction(
                ActionProvider.COMMAND_REBUILD,
                NbStrings.getRebuildCommandCaption()));

        ExtensionActions extActions = getExtensionActions();
        projectActions.addAll(extActions.getBuildActions());

        projectActions.add(customTasksAction);
        projectActions.add(tasksAction);
        projectActions.add(null);
        tryAddActionObj("Actions/Project/org-netbeans-modules-project-ui-problems-BrokenProjectActionFactory.instance", projectActions);
        projectActions.add(createProjectAction(
                GradleActionProvider.COMMAND_RELOAD,
                NbStrings.getReloadCommandCaption(true)));
        // Add the commented code below to provide a "Refresh project node" action.
        // It was removed because it confused many, users can't easily distinguish it from "Reload project".
        // projectActions.add(NodeUtils.getRefreshNodeAction(this, NbStrings.getRefreshNodeCommandCaption()));
        projectActions.add(createProjectAction(
                GradleActionProvider.COMMAND_SET_AS_MAIN_PROJECT,
                NbStrings.getSetAsMainCaption()));
        projectActions.addAll(extActions.getProjectManagementActions());
        projectActions.add(CommonProjectActions.closeProjectAction());
        projectActions.add(null);
        projectActions.add(new DeleteProjectAction(project));
        projectActions.add(null);
        tryAddActionObj("Actions/Edit/org-openide-actions-FindAction.instance", projectActions);
        projectActions.addAll(Utilities.actionsForPath("Projects/Actions"));
        projectActions.add(null);
        projectActions.add(CommonProjectActions.customizeProjectAction());

        return projectActions.toArray(new Action[projectActions.size()]);
    }

    private ExtensionActions getExtensionActions() {
        ExtensionActions result = new ExtensionActions();

        for (NbGradleExtensionRef extensionRef: project.getExtensions().getExtensionRefs()) {
            ExtensionActions actions = getActionsOfExtension(extensionRef);
            result.mergeActions(actions);
        }

        if (!result.getBuildActions().isEmpty()) {
            result.addActionSeparator(GradleActionType.BUILD_ACTION);
        }
        return result;
    }

    private static ExtensionActions getActionsOfExtension(NbGradleExtensionRef extension) {
        Lookup extensionLookup = extension.getExtensionLookup();
        Collection<? extends GradleProjectContextActions> actionQueries
                = extensionLookup.lookupAll(GradleProjectContextActions.class);

        ExtensionActions result = new ExtensionActions();
        for (GradleProjectContextActions actionQuery: actionQueries) {
            result.addAllActions(trimNulls(actionQuery.getContextActions()));
        }
        return result;
    }

    private static <T> List<T> trimNulls(List<T> list) {
        int firstNonNullIndex = 0;

        for (T element: list) {
            if (element != null) {
                break;
            }

            firstNonNullIndex++;
        }

        int afterLastNonNullIndex = list.size();
        ListIterator<T> backItr = list.listIterator(afterLastNonNullIndex);
        while (backItr.hasPrevious()) {
            T element = backItr.previous();
            if (element != null) {
                break;
            }
            afterLastNonNullIndex--;
        }

        return list.subList(firstNonNullIndex, afterLastNonNullIndex);
    }

    private static void executeCommandTemplate(
            NbGradleProject project,
            GradleCommandTemplate command) {
        CustomCommandActions actions = command.isBlocking()
                ? CustomCommandActions.OTHER
                : CustomCommandActions.BUILD;

        project.getGradleCommandExecutor().executeCommand(command, actions);
    }

    private static void tryAddActionObj(String objectPath, List<? super Action> actionList) {
        Action action = NamedServicesProvider.getConfigObject(objectPath, Action.class);
        if (action != null) {
            actionList.add(action);
        }
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    @SuppressWarnings("serial") // don't care about serialization
    private static class CustomTasksActionMenu extends AbstractAction implements Presenter.Popup {
        private final NbGradleProject project;
        private JMenu cachedMenu;

        public CustomTasksActionMenu(NbGradleProject project) {
            this.project = project;
            this.cachedMenu = null;
        }

        @Override
        public JMenuItem getPopupPresenter() {
            if (cachedMenu == null) {
                cachedMenu = createMenu();
            }
            return cachedMenu;
        }

        private JMenu createMenu() {
            JMenu menu = new JMenu(NbStrings.getCustomTasksCommandCaption());
            final CustomTasksMenuBuilder builder = new CustomTasksMenuBuilder(project, menu);
            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    builder.updateMenuContent();
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });
            return menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    private static class CustomTasksMenuBuilder {
        private final NbGradleProject project;
        private final JMenu menu;
        private PredefinedTasks lastUsedTasks;
        private NbGradleModel lastUsedModule;

        public CustomTasksMenuBuilder(NbGradleProject project, JMenu menu) {
            this.project = Objects.requireNonNull(project, "project");
            this.menu = Objects.requireNonNull(menu, "menu");
            this.lastUsedTasks = null;
            this.lastUsedModule = null;
        }


        public void updateMenuContent() {
            PredefinedTasks commonTasks = project.getCommonProperties().customTasks().getActiveValue();
            NbGradleModel mainModule = project.currentModel().getValue();
            if (lastUsedTasks == commonTasks && lastUsedModule == mainModule) {
                return;
            }

            lastUsedTasks = commonTasks;
            lastUsedModule = mainModule;

            List<PredefinedTask> commonTasksList = new ArrayList<>(commonTasks.getTasks());
            commonTasksList.sort(Comparator.comparing(PredefinedTask::getDisplayName, StringUtils.STR_CMP::compare));

            boolean hasCustomTasks = false;
            menu.removeAll();

            StringResolver taskNameResolver = StringResolvers
                    .getDefaultResolverSelector()
                    .getProjectResolver(project, Lookup.EMPTY);
            for (final PredefinedTask task: commonTasksList) {
                if (!task.isTasksExistsIfRequired(project, taskNameResolver)) {
                    continue;
                }

                JMenuItem menuItem = new JMenuItem(task.getDisplayName());
                menuItem.addActionListener((ActionEvent e) -> {
                    executeCommandTemplate(project, task.toCommandTemplate());
                });
                menu.add(menuItem);
                hasCustomTasks = true;
            }
            if (hasCustomTasks) {
                menu.addSeparator();
            }
            menu.add(new CustomTaskAction(project));
        }
    }

    @SuppressWarnings("serial") // don't care about serialization
    private static class CustomTaskAction extends AbstractAction {
        private final NbGradleProject project;

        public CustomTaskAction(NbGradleProject project) {
            super(NbStrings.getCustomTasksCommandCaption());
            this.project = project;
        }

        private PredefinedTask tryCreateTaskDef(
                CustomActionPanel actionPanel,
                String displayName,
                boolean tasksMustExist) {
            actionPanel.setTasksMustExist(tasksMustExist);
            return actionPanel.tryGetPredefinedTask(displayName);
        }

        private String doSaveTask(CustomActionPanel actionPanel) {
            AddNewTaskPanel panel = new AddNewTaskPanel();

            DialogDescriptor dlgDescriptor = new DialogDescriptor(
                    panel,
                    NbStrings.getAddNewTaskDlgTitle(),
                    true,
                    new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                    DialogDescriptor.OK_OPTION,
                    DialogDescriptor.BOTTOM_ALIGN,
                    null,
                    null);
            Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
            dlg.pack();
            dlg.setVisible(true);
            if (dlgDescriptor.getValue() != DialogDescriptor.OK_OPTION) {
                return null;
            }
            String displayName = panel.getDisplayName();
            if (displayName.isEmpty()) {
                return null;
            }

            PredefinedTask newTaskDef = tryCreateTaskDef(actionPanel, displayName, true);
            if (newTaskDef == null) {
                return null;
            }

            if (!newTaskDef.isTasksExistsIfRequired(project, Lookup.EMPTY)) {
                newTaskDef = tryCreateTaskDef(actionPanel, displayName, false);
            }

            addNewCommonTaskTask(newTaskDef);
            return displayName;
        }

        private void addNewCommonTaskTask(final PredefinedTask newTaskDef) {
            NbGradleCommonProperties commonProperties = project.getCommonProperties();
            MutableProperty<PredefinedTasks> commonTasks = commonProperties.customTasks().getForActiveProfile();

            List<PredefinedTask> currentTasks = commonTasks.getValue().getTasks();
            List<PredefinedTask> newTasks = new ArrayList<>(currentTasks.size() + 1);
            newTasks.addAll(currentTasks);
            newTasks.add(newTaskDef);

            commonTasks.setValue(new PredefinedTasks(newTasks));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CustomActionPanel panel = new CustomActionPanel();
            JButton executeButton = new JButton(NbStrings.getExecuteLabel());
            JButton saveAndExecuteButton = new JButton(NbStrings.getSaveAndExecuteLabel());

            final DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getCustomTaskDlgTitle(),
                true,
                new Object[]{executeButton, saveAndExecuteButton, DialogDescriptor.CANCEL_OPTION},
                executeButton,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
            PropertySource<Boolean> validInput = panel.validInput();
            AutoDisplayState.addSwingStateListener(validInput,
                    AutoDisplayState.componentDisabler(executeButton, saveAndExecuteButton));

            dlgDescriptor.setValid(validInput.getValue());

            Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
            dlg.setVisible(true);

            String displayName;
            boolean doExecute = false;
            boolean okToClose;
            do {
                displayName = null;
                okToClose = true;
                Object selectedButton = dlgDescriptor.getValue();

                if (saveAndExecuteButton == selectedButton) {
                    displayName = doSaveTask(panel);
                    okToClose = displayName != null;

                    if (!okToClose) {
                        dlg.setVisible(true);
                    }
                    doExecute = true;
                }
                else if (executeButton == selectedButton) {
                    doExecute = true;
                }
            } while (!okToClose);

            if (doExecute) {
                final GradleCommandTemplate commandTemplate = panel.tryGetGradleCommand(displayName);
                if (commandTemplate != null) {
                    executeCommandTemplate(project, commandTemplate);
                }
            }
        }
    }

    @SuppressWarnings("serial") // don't care about serialization
    private static class TasksActionMenu extends AbstractAction implements Presenter.Popup {
        private final NbGradleProject project;
        private JMenu cachedMenu;

        public TasksActionMenu(NbGradleProject project) {
            this.project = project;
            this.cachedMenu = null;
        }

        @Override
        public JMenuItem getPopupPresenter() {
            if (cachedMenu == null) {
                cachedMenu = createMenu();
            }
            return cachedMenu;
        }

        private JMenu createMenu() {
            JMenu menu = new JMenu(NbStrings.getTasksMenuCaption());
            final TasksMenuBuilder builder = new TasksMenuBuilder(project, menu);
            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    builder.updateMenuContent();
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });
            return menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    private static class TasksMenuBuilder {
        private final NbGradleProject project;
        private final JMenu menu;
        private NbGradleModel lastUsedModel;

        public TasksMenuBuilder(NbGradleProject project, JMenu menu) {
            this.project = Objects.requireNonNull(project, "project");
            this.menu = Objects.requireNonNull(menu, "menu");
            this.lastUsedModel = null;
        }

        private void addToMenu(JMenu rootMenu, List<GradleTaskTree> rootNodes) {
            for (GradleTaskTree root: rootNodes) {
                List<GradleTaskTree> children = root.getChildren();

                JMenuItem toAdd;
                if (children.isEmpty()) {
                    toAdd = new JMenuItem(root.getCaption());
                }
                else {
                    JMenu subMenu = new JMenu(root.getCaption());
                    toAdd = subMenu;
                    addToMenu(subMenu, children);
                }

                final GradleTaskID taskID = root.getTaskID();
                if (taskID != null) {
                    toAdd.addActionListener((ActionEvent e) -> {
                        GradleCommandTemplate.Builder command
                                = new GradleCommandTemplate.Builder("", Arrays.asList(taskID.getFullName()));

                        executeCommandTemplate(project, command.create());
                    });
                }

                rootMenu.add(toAdd);
            }
        }

        public void updateMenuContent() {
            NbGradleModel projectModel = project.currentModel().getValue();
            if (lastUsedModel == projectModel) {
                return;
            }

            lastUsedModel = projectModel;

            Collection<GradleTaskID> tasks = projectModel.getMainProject().getTasks();

            menu.removeAll();
            addToMenu(menu, GradleTaskTree.createTaskTree(tasks));
        }
    }

    private static final class ExtensionActions {
        private final List<Action> buildActions;
        private final List<Action> projectManagementActions;
        private GradleActionType lastActionType;

        public ExtensionActions() {
            this.buildActions = new ArrayList<>();
            this.projectManagementActions = new ArrayList<>();
            this.lastActionType = GradleActionType.BUILD_ACTION;
        }

        public List<Action> getBuildActions() {
            return buildActions;
        }

        public List<Action> getProjectManagementActions() {
            return projectManagementActions;
        }

        private List<Action> getListForActionKind(GradleActionType actionType) {
            switch (actionType) {
                case BUILD_ACTION:
                    return buildActions;
                case PROJECT_MANAGEMENT_ACTION:
                    return projectManagementActions;
                default:
                    throw new AssertionError(actionType.name());
            }
        }

        private void addActionSeparator(GradleActionType actionType) {
            getListForActionKind(actionType).add(null);
        }

        private void addAction(GradleActionType actionType, Action action) {
            lastActionType = actionType;
            getListForActionKind(actionType).add(action);
        }

        private GradleActionType getActionTypeForAction(Action action) {
            if (action == null) {
                return lastActionType;
            }

            GradleProjectAction annotation = action.getClass().getAnnotation(GradleProjectAction.class);
            return annotation != null
                    ? annotation.value()
                    : GradleActionType.BUILD_ACTION;
        }

        public void addAction(Action action) {
            addAction(getActionTypeForAction(action), action);
        }

        public void addAllActions(Collection<? extends Action> actions) {
            for (Action action: actions) {
                addAction(action);
            }
        }

        public void mergeActions(ExtensionActions actions) {
            if (!actions.buildActions.isEmpty()) {
                buildActions.add(null);
                buildActions.addAll(actions.buildActions);
            }

            projectManagementActions.addAll(actions.projectManagementActions);
        }
    }
}
