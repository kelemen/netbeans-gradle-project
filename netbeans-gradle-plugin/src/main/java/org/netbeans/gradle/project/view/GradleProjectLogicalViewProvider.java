package org.netbeans.gradle.project.view;

import java.awt.Dialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
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
import java.util.StringTokenizer;
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
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfo.Kind;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.api.task.CustomCommandActions;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.AddNewTaskPanel;
import org.netbeans.gradle.project.properties.OldMutableProperty;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeNotFoundException;
import org.openide.nodes.NodeOp;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.ProxyLookup;

public final class GradleProjectLogicalViewProvider
implements
        LogicalViewProvider, ModelRefreshListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;

    private final ListenerManager<ModelRefreshListener> childRefreshListeners;
    private final AtomicReference<Collection<ModelRefreshListener>> listenersToFinalize;
    private final ListenerManager<Runnable> refreshRequestListeners;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
        this.childRefreshListeners = new CopyOnTriggerListenerManager<>();
        this.listenersToFinalize = new AtomicReference<>(null);
        this.refreshRequestListeners = new CopyOnTriggerListenerManager<>();
    }

    public void refreshProjectNode() {
        EventListeners.dispatchRunnable(refreshRequestListeners);
    }

    public ListenerRef addRefreshRequestListeners(Runnable listener) {
        return refreshRequestListeners.registerListener(listener);
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

        final ListenerRef modelListenerRef = project.addModelChangeListener(new Runnable() {
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
                infoListenerRef.unregister();
                modelListenerRef.unregister();
            }
        });

        return result;
    }

    private Children createChildren() {
        return Children.create(new GradleProjectChildFactory(project, this), false);
    }

    private Lookup createLookup(Node rootNode) {
        return new ProxyLookup(
                project.getLookup(),
                rootNode.getLookup());
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

        for (NbGradleExtensionRef extensionRef: project.getExtensionRefs()) {
            ExtensionActions actions = getActionsOfExtension(extensionRef);
            result.mergeActions(actions);
        }

        if (!result.getBuildActions().isEmpty()) {
            result.addActionSeparator(GradleActionType.BUILD_ACTION);
        }
        return result;
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    private final class GradleProjectNode extends FilterNode {
        @SuppressWarnings("VolatileArrayField")
        private volatile Action[] actions;

        public GradleProjectNode(Node node) {
            super(node, createChildren(), createLookup(node));

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
            projectActions.add(new RefreshNodesAction());
            projectActions.addAll(extActions.getProjectManagementActions());
            projectActions.add(CommonProjectActions.closeProjectAction());
            projectActions.add(null);
            projectActions.add(new DeleteProjectAction(project));
            projectActions.add(null);
            projectActions.addAll(Utilities.actionsForPath("Projects/Actions"));
            projectActions.add(null);
            projectActions.add(CommonProjectActions.customizeProjectAction());

            this.actions = projectActions.toArray(new Action[projectActions.size()]);
        }

        public void fireModelChange() {
            fireDisplayNameChange(null, null);
            fireShortDescriptionChange(null, null);
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
            return project.getDisplayName();
        }

        @Override
        public String getShortDescription() {
            return project.getDescription();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        // The implementation of this method is mostly a copy-paste from the
        // Maven plugin. I didn't take the time to fully understand it.
        if (target instanceof FileObject) {
            FileObject fileObject = (FileObject)target;

            Node[] nodes = root.getChildren().getNodes(false);
            for (Node child: nodes) {
                Node found = PackageView.findPath(child, fileObject);
                if (found != null) {
                    return found;
                }
            }
            for (Node node: nodes) {
                for (Node childNode: node.getChildren().getNodes(false)) {
                    Node result = PackageView.findPath(childNode, fileObject);
                    if (result != null) {
                        return result;
                    }
                    Node found = findNodeByFileDataObject(childNode, fileObject);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private Node findNodeByFileDataObject(Node node, FileObject fo) {
        FileObject xfo = node.getLookup().lookup(FileObject.class);
        if (xfo == null) {
            DataObject dobj = node.getLookup().lookup(DataObject.class);
            if (dobj != null) {
                xfo = dobj.getPrimaryFile();
            }
        }
        if (xfo != null) {
            if ((xfo.equals(fo))) {
                return node;
            }
            else if (FileUtil.isParentOf(xfo, fo)) {
                FileObject folder = fo.isFolder() ? fo : fo.getParent();
                String relPath = FileUtil.getRelativePath(xfo, folder);
                List<String> path = new ArrayList<>();
                StringTokenizer strtok = new StringTokenizer(relPath, "/");
                while (strtok.hasMoreTokens()) {
                    String token = strtok.nextToken();
                    path.add(token);
                }
                try {
                    Node folderNode = folder.equals(xfo) ? node : NodeOp.findPath(node, Collections.enumeration(path));
                    if (fo.isFolder()) {
                        return folderNode;
                    }
                    else {
                        Node[] childs = folderNode.getChildren().getNodes(false);
                        for (Node child: childs) {
                            DataObject dobj = child.getLookup().lookup(DataObject.class);
                            if (dobj != null && dobj.getPrimaryFile().getNameExt().equals(fo.getNameExt())) {
                                return child;
                            }
                        }
                    }
                } catch (NodeNotFoundException e) {
                    // OK, never mind
                }
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

        private PredefinedTask createTaskDef(
                CustomActionPanel actionPanel,
                String displayName,
                boolean tasksMustExist) {
            String[] rawTaskNames = actionPanel.getTasks();
            List<PredefinedTask.Name> names = new ArrayList<>(rawTaskNames.length);
            for (String name: rawTaskNames) {
                names.add(new PredefinedTask.Name(name, tasksMustExist));
            }

            return new PredefinedTask(
                    displayName,
                    names,
                    Arrays.asList(actionPanel.getArguments()),
                    Arrays.asList(actionPanel.getJvmArguments()),
                    actionPanel.isNonBlocking());
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

            PredefinedTask newTaskDef = createTaskDef(actionPanel, displayName, true);
            if (!newTaskDef.isTasksExistsIfRequired(project, Lookup.EMPTY)) {
                newTaskDef = createTaskDef(actionPanel, displayName, false);
            }

            OldMutableProperty<List<PredefinedTask>> commonTasks = project.getProperties().getCommonTasks();

            List<PredefinedTask> newTasks = new LinkedList<>(commonTasks.getValue());
            newTasks.add(newTaskDef);
            commonTasks.setValue(newTasks);
            return displayName;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CustomActionPanel panel = new CustomActionPanel();
            JButton executeButton = new JButton(NbStrings.getExecuteLabel());
            JButton saveAndExecuteButton = new JButton(NbStrings.getSaveAndExecuteLabel());

            DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getCustomTaskDlgTitle(),
                true,
                new Object[]{executeButton, saveAndExecuteButton, DialogDescriptor.CANCEL_OPTION},
                executeButton,
                DialogDescriptor.BOTTOM_ALIGN,
                null,
                null);
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
    private class RefreshNodesAction extends AbstractAction {
        public RefreshNodesAction() {
            super(NbStrings.getRefreshNodeCommandCaption());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            refreshProjectNode();
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
        private static final Collator STR_CMP = Collator.getInstance();

        private final NbGradleProject project;
        private final JMenu menu;
        private List<PredefinedTask> lastUsedTasks;
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
            List<PredefinedTask> commonTasks = project.getProperties().getCommonTasks().getValue();
            NbGradleModel mainModule = project.getAvailableModel();
            if (lastUsedTasks == commonTasks && lastUsedModule == mainModule) {
                return;
            }

            lastUsedTasks = commonTasks;
            lastUsedModule = mainModule;

            commonTasks = new ArrayList<>(commonTasks);
            Collections.sort(commonTasks, new Comparator<PredefinedTask>() {
                @Override
                public int compare(PredefinedTask o1, PredefinedTask o2) {
                    String name1 = o1.getDisplayName();
                    String name2 = o2.getDisplayName();
                    return STR_CMP.compare(name1, name2);
                }
            });

            boolean hasCustomTasks = false;
            menu.removeAll();

            TaskVariableMap varReplaceMap = project.getVarReplaceMap(Lookup.EMPTY);
            for (final PredefinedTask task: commonTasks) {
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
            NbGradleModel projectModel = project.getAvailableModel();
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
