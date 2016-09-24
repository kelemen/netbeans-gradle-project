package org.netbeans.gradle.project.view;

import java.awt.Dialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.AutoDisplayState;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfo.Kind;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.api.nodes.NodeRefresher;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;
import org.netbeans.gradle.project.properties.ui.AddNewTaskPanel;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.PathFinder;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeEvent;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.implspi.NamedServicesProvider;

public final class GradleProjectLogicalViewProvider
implements
        LogicalViewProvider, ModelRefreshListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;

    private final ListenerManager<ModelRefreshListener> childRefreshListeners;
    private final AtomicReference<Collection<ModelRefreshListener>> listenersToFinalize;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
        this.childRefreshListeners = new CopyOnTriggerListenerManager<>();
        this.listenersToFinalize = new AtomicReference<>(null);
    }

    public ListenerRef addChildModelRefreshListener(final ModelRefreshListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        return childRefreshListeners.registerListener(listener);
    }

    @Override
    public void startRefresh() {
        final List<ModelRefreshListener> listeners = new LinkedList<>();
        childRefreshListeners.onEvent(new EventDispatcher<ModelRefreshListener, Void>() {
            @Override
            public void onEvent(ModelRefreshListener eventListener, Void arg) {
                eventListener.startRefresh();
                listeners.add(eventListener);
            }
        }, null);

        Collection<ModelRefreshListener> prevListeners = listenersToFinalize.getAndSet(listeners);
        if (prevListeners != null) {
            LOGGER.warning("startRefresh/endRefresh mismatch.");
        }
    }

    @Override
    public void endRefresh(boolean extensionsChanged) {
        Collection<ModelRefreshListener> listeners = listenersToFinalize.getAndSet(null);
        if (listeners == null) {
            return;
        }

        for (ModelRefreshListener listener: listeners) {
            listener.endRefresh(extensionsChanged);
        }
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());

        final GradleProjectNode result = new GradleProjectNode(projectFolder.getNodeDelegate().cloneNode());

        PropertySource<String> displayName = PropertyFactory.lazilyNotifiedSource(project.displayName());
        PropertySource<String> description = PropertyFactory.lazilyNotifiedSource(project.description());

        final ListenerRef displayNameRef = displayName.addChangeListener(new Runnable() {
            @Override
            public void run() {
                result.fireDisplayNameChange();
            }
        });
        final ListenerRef descriptionRef = description.addChangeListener(new Runnable() {
            @Override
            public void run() {
                result.fireShortDescriptionChange();
            }
        });
        final ListenerRef modelListenerRef = project.currentModel().addChangeListener(new Runnable() {
            @Override
            public void run() {
                result.fireModelChange();
            }
        });
        final ListenerRef infoListenerRef = project.getProjectInfoManager().addChangeListener(new Runnable() {
            @Override
            public void run() {
                result.fireInfoChangeEvent();
            }
        });
        result.addNodeListener(new NodeAdapter(){
            @Override
            public void nodeDestroyed(NodeEvent ev) {
                displayNameRef.unregister();
                descriptionRef.unregister();
                infoListenerRef.unregister();
                modelListenerRef.unregister();
            }
        });

        return result;
    }

    private Lookup createLookup(GradleProjectChildFactory childFactory, Children children) {
        NodeRefresher nodeRefresher = NodeUtils.defaultNodeRefresher(children, childFactory);
        return new ProxyLookup(
                project.getLookup(),
                Lookups.fixed(nodeRefresher, project.getProjectDirectory()));
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

    private static void tryAddActionObj(String objectPath, List<? super Action> actionList) {
        Action action = NamedServicesProvider.getConfigObject(objectPath, Action.class);
        if (action != null) {
            actionList.add(action);
        }
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    private final class GradleProjectNode extends FilterNode {
        @SuppressWarnings("VolatileArrayField")
        private volatile Action[] actions;

        public GradleProjectNode(Node node) {
            this(node, new GradleProjectChildFactory(project, GradleProjectLogicalViewProvider.this));
        }

        private GradleProjectNode(Node node, GradleProjectChildFactory childFactory) {
            this(node, childFactory, Children.create(childFactory, false));
        }

        private GradleProjectNode(
                Node node,
                GradleProjectChildFactory childFactory,
                org.openide.nodes.Children children) {
            // Do not add lookup of "node" because that might fool NB to believe that multiple projects are selected.
            super(node, children, createLookup(childFactory, children));

            updateActionsList();
        }

        private void updateActionsList() {
            TasksActionMenu tasksAction = new TasksActionMenu(project);
            CustomTasksActionMenu customTasksAction = new CustomTasksActionMenu(project);

            List<Action> projectActions = new LinkedList<>();
            projectActions.add(CommonProjectActions.newFileAction());
            projectActions.add(null);
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_RUN,
                    NbStrings.getRunCommandCaption()));
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_DEBUG,
                    NbStrings.getDebugCommandCaption()));
            projectActions.add(null);
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_BUILD,
                    NbStrings.getBuildCommandCaption()));
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_TEST,
                    NbStrings.getTestCommandCaption()));
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_CLEAN,
                    NbStrings.getCleanCommandCaption()));
            projectActions.add(createProjectAction(
                    ActionProvider.COMMAND_REBUILD,
                    NbStrings.getRebuildCommandCaption()));

            ExtensionActions extActions = getExtensionActions();
            projectActions.addAll(extActions.getBuildActions());

            projectActions.add(customTasksAction);
            projectActions.add(tasksAction);
            projectActions.add(null);
            projectActions.add(createProjectAction(
                    GradleActionProvider.COMMAND_RELOAD,
                    NbStrings.getReloadCommandCaption()));
            // Add the commented code below to provide a "Refresh project node" action.
            // It was removed because it confused many, users can't easily distinguish it from "Reload project".
            // projectActions.add(NodeUtils.getRefreshNodeAction(this, NbStrings.getRefreshNodeCommandCaption()));
            projectActions.addAll(extActions.getProjectManagementActions());
            projectActions.add(CommonProjectActions.closeProjectAction());
            projectActions.add(null);
            projectActions.add(new DeleteProjectAction(project));
            projectActions.add(null);
            tryAddActionObj("Actions/Edit/org-openide-actions-FindAction.instance", projectActions);
            projectActions.addAll(Utilities.actionsForPath("Projects/Actions"));
            projectActions.add(null);
            projectActions.add(CommonProjectActions.customizeProjectAction());

            this.actions = projectActions.toArray(new Action[projectActions.size()]);
        }

        public void fireDisplayNameChange() {
            fireDisplayNameChange(null, null);
        }

        public void fireShortDescriptionChange() {
            fireShortDescriptionChange(null, null);
        }

        public void fireModelChange() {
            updateActionsList();
        }

        public void fireInfoChangeEvent() {
            fireIconChange();
            fireOpenedIconChange();
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        private void appendHtmlList(String caption, List<String> toAdd, StringBuilder result) {
            if (toAdd.isEmpty()) {
                return;
            }

            result.append("<B>");
            result.append(caption);
            result.append("</B>:");
            result.append("<ul>\n");
            for (String info: toAdd) {
                result.append("<li>");
                // TODO: quote strings, so that they are valid for html
                result.append(info);
                result.append("</li>\n");
            }
            result.append("</ul>\n");
        }

        @Override
        public Image getIcon(int type) {
            Image icon = NbIcons.getGradleIcon();
            Collection<ProjectInfo> infos = project.getProjectInfoManager().getInformations();
            if (!infos.isEmpty()) {
                Map<ProjectInfo.Kind, List<String>> infoMap
                        = new EnumMap<>(ProjectInfo.Kind.class);

                for (ProjectInfo.Kind kind: ProjectInfo.Kind.values()) {
                    infoMap.put(kind, new LinkedList<String>());
                }

                Kind mostImportantKind = Kind.INFO;
                for (ProjectInfo info: infos) {
                    for (ProjectInfo.Entry entry: info.getEntries()) {
                        Kind kind = entry.getKind();
                        if (mostImportantKind.getImportance() < kind.getImportance()) {
                            mostImportantKind = kind;
                        }
                        infoMap.get(kind).add(entry.getInfo());
                    }
                }

                StringBuilder completeText = new StringBuilder(1024);
                appendHtmlList(NbStrings.getErrorCaption(), infoMap.get(ProjectInfo.Kind.ERROR), completeText);
                appendHtmlList(NbStrings.getWarningCaption(), infoMap.get(ProjectInfo.Kind.WARNING), completeText);
                appendHtmlList(NbStrings.getInfoCaption(), infoMap.get(ProjectInfo.Kind.INFO), completeText);

                icon = ImageUtilities.addToolTipToImage(icon, completeText.toString());
                if (mostImportantKind.getImportance() >= ProjectInfo.Kind.WARNING.getImportance()) {
                    icon = ImageUtilities.mergeImages(icon, NbIcons.getWarningBadge(), 0, 0);
                }
            }
            return icon;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.displayName().getValue();
        }

        @Override
        public String getShortDescription() {
            return project.description().getValue();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        if (target == null) {
            return null;
        }

        FileObject targetFile = NodeUtils.tryGetFileSearchTarget(target);

        Node[] children = root.getChildren().getNodes(true);
        for (Node child: children) {
            boolean hasNodeFinder = false;
            for (PathFinder nodeFinder: child.getLookup().lookupAll(PathFinder.class)) {
                hasNodeFinder = true;

                Node result = nodeFinder.findPath(child, target);
                if (result != null) {
                    return result;
                }
            }

            if (hasNodeFinder) {
                continue;
            }

            // This will always return {@code null} because PackageView
            // asks for PathFinder as well but since it is not in its
            // specification, we won't rely on this.
            Node result = PackageView.findPath(child, target);
            if (result == null && targetFile != null) {
                result = NodeUtils.findChildFileOfFolderNode(child, targetFile);
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static void executeCommandTemplate(
            NbGradleProject project,
            GradleCommandTemplate command) {
        CustomCommandActions actions = command.isBlocking()
                ? CustomCommandActions.OTHER
                : CustomCommandActions.BUILD;

        project.getLookup().lookup(GradleCommandExecutor.class)
                .executeCommand(command, actions);
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
            ExceptionHelper.checkNotNullArgument(project, "project");
            ExceptionHelper.checkNotNullArgument(menu, "menu");

            this.project = project;
            this.menu = menu;
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
            Collections.sort(commonTasksList, new Comparator<PredefinedTask>() {
                @Override
                public int compare(PredefinedTask o1, PredefinedTask o2) {
                    String name1 = o1.getDisplayName();
                    String name2 = o2.getDisplayName();
                    return StringUtils.STR_CMP.compare(name1, name2);
                }
            });

            boolean hasCustomTasks = false;
            menu.removeAll();

            TaskVariableMap varReplaceMap = project.getVarReplaceMap(Lookup.EMPTY);
            for (final PredefinedTask task: commonTasksList) {
                if (!task.isTasksExistsIfRequired(project, varReplaceMap)) {
                    continue;
                }

                JMenuItem menuItem = new JMenuItem(task.getDisplayName());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        executeCommandTemplate(project, task.toCommandTemplate());
                    }
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
            ExceptionHelper.checkNotNullArgument(project, "project");
            ExceptionHelper.checkNotNullArgument(menu, "menu");

            this.project = project;
            this.menu = menu;
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
                    toAdd.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            GradleCommandTemplate.Builder command
                                    = new GradleCommandTemplate.Builder("", Arrays.asList(taskID.getFullName()));

                            executeCommandTemplate(project, command.create());
                        }
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
            this.buildActions = new LinkedList<>();
            this.projectManagementActions = new LinkedList<>();
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
