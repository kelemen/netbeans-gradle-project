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
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfo.Kind;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbGradleTask;
import org.netbeans.gradle.project.properties.AddNewTaskPanel;
import org.netbeans.gradle.project.properties.MutableProperty;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.GradleTasks;
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

public final class GradleProjectLogicalViewProvider implements LogicalViewProvider {
    private final NbGradleProject project;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());

        final GradleProjectNode result = new GradleProjectNode(projectFolder.getNodeDelegate().cloneNode());
        final ChangeListener infoChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                result.fireInfoChangeEvent();
            }
        };
        final ChangeListener modelListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                result.fireModelChange();
            }
        };

        project.addModelChangeListener(modelListener);
        project.getProjectInfoManager().addChangeListener(infoChangeListener);
        result.addNodeListener(new NodeAdapter(){
            @Override
            public void nodeDestroyed(NodeEvent ev) {
                project.getProjectInfoManager().removeChangeListener(infoChangeListener);
                project.removeModelChangeListener(modelListener);
            }
        });

        return result;
    }

    private Children createChildren() {
        return Children.create(new GradleProjectChildFactory(project), false);
    }

    private Lookup createLookup(Node rootNode) {
        return new ProxyLookup(
                project.getLookup(),
                rootNode.getLookup());
    }

    private static List<Action> getActionsOfExtension(GradleProjectExtension extension) {
        Lookup extensionLookup = extension.getExtensionLookup();
        Collection<? extends GradleProjectContextActions> actionQueries
                = extensionLookup.lookupAll(GradleProjectContextActions.class);

        List<Action> result = new LinkedList<Action>();
        for (GradleProjectContextActions actionQuery: actionQueries) {
            result.addAll(actionQuery.getContextActions());
        }
        return result;
    }

    private List<Action> getExtensionActions() {
        List<Action> extensionActions = new LinkedList<Action>();
        for (GradleProjectExtension extension: project.getExtensions()) {
            List<Action> actions = getActionsOfExtension(extension);

            if (!actions.isEmpty()) {
                extensionActions.add(null);
                extensionActions.addAll(actions);
            }
        }
        if (!extensionActions.isEmpty()) {
            extensionActions.add(null);
        }
        return extensionActions;
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    private final class GradleProjectNode extends FilterNode {
        private final Action[] actions;

        public GradleProjectNode(Node node) {
            super(node, createChildren(), createLookup(node));

            TasksActionMenu tasksAction = new TasksActionMenu(project);
            CustomTasksActionMenu customTasksAction = new CustomTasksActionMenu(project);

            List<Action> projectActions = new LinkedList<Action>();
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
            projectActions.add(createProjectAction(
                    JavaProjectConstants.COMMAND_JAVADOC,
                    NbStrings.getJavadocCommandCaption()));

            projectActions.addAll(getExtensionActions());

            projectActions.add(customTasksAction);
            projectActions.add(tasksAction);
            projectActions.add(null);
            projectActions.add(createProjectAction(
                    GradleActionProvider.COMMAND_RELOAD,
                    NbStrings.getReloadCommandCaption()));
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
                        = new EnumMap<ProjectInfo.Kind, List<String>>(ProjectInfo.Kind.class);

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

            // fallback if not found by PackageView.
            for (int i = 0; i < nodes.length; i++) {
                for (Node node: nodes[i].getChildren().getNodes(false)) {
                    Node result = PackageView.findPath(node, fileObject);
                    if (result != null) {
                        return result;
                    }
                    Node found = findNodeByFileDataObject(node, fileObject);
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
                List<String> path = new ArrayList<String>();
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
                        for (int j = 0; j < childs.length; j++) {
                            DataObject dobj = childs[j].getLookup().lookup(DataObject.class);
                            if (dobj != null && dobj.getPrimaryFile().getNameExt().equals(fo.getNameExt())) {
                                return childs[j];
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
            List<PredefinedTask.Name> names = new ArrayList<PredefinedTask.Name>(rawTaskNames.length);
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

        private boolean doSaveTask(CustomActionPanel actionPanel) {
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
                return false;
            }
            String displayName = panel.getDisplayName();
            if (displayName.isEmpty()) {
                return false;
            }

            PredefinedTask newTaskDef = createTaskDef(actionPanel, displayName, true);
            if (newTaskDef.tryCreateTaskDef(project, Lookup.EMPTY) == null) {
                newTaskDef = createTaskDef(actionPanel, displayName, false);
            }

            MutableProperty<List<PredefinedTask>> commonTasks = project.getProperties().getCommonTasks();

            List<PredefinedTask> newTasks = new LinkedList<PredefinedTask>(commonTasks.getValue());
            newTasks.add(newTaskDef);
            commonTasks.setValue(newTasks);
            return true;
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

            boolean doExecute = false;
            boolean okToClose;
            do {
                okToClose = true;
                Object selectedButton = dlgDescriptor.getValue();

                if (saveAndExecuteButton == selectedButton) {
                    okToClose = doSaveTask(panel);
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
                final PredefinedTask predefinedTask = panel.tryGetPredefinedTask("");
                if (predefinedTask != null) {
                    GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
                        @Override
                        public GradleTaskDef call() {
                            return predefinedTask.tryCreateTaskDef(project, Lookup.EMPTY);
                        }
                    }).run();
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
        private static final Collator STR_CMP = Collator.getInstance();

        private final NbGradleProject project;
        private final JMenu menu;
        private List<PredefinedTask> lastUsedTasks;
        private NbGradleModule lastUsedModule;

        public CustomTasksMenuBuilder(NbGradleProject project, JMenu menu) {
            if (project == null) throw new NullPointerException("project");
            if (menu == null) throw new NullPointerException("menu");

            this.project = project;
            this.menu = menu;
            this.lastUsedTasks = null;
            this.lastUsedModule = null;
        }


        public void updateMenuContent() {
            List<PredefinedTask> commonTasks = project.getProperties().getCommonTasks().getValue();
            NbGradleModule mainModule = project.getAvailableModel().getMainModule();
            if (lastUsedTasks == commonTasks && lastUsedModule == mainModule) {
                return;
            }

            lastUsedTasks = commonTasks;
            lastUsedModule = mainModule;

            commonTasks = new ArrayList<PredefinedTask>(commonTasks);
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

            for (final PredefinedTask task: commonTasks) {
                if (task.tryCreateTaskDef(project, Lookup.EMPTY) == null) {
                    continue;
                }

                JMenuItem menuItem = new JMenuItem(task.getDisplayName());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // recreate the task on every actionPerformed because
                        // the project might have changed since the menu item
                        // was created.
                        GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
                            @Override
                            public GradleTaskDef call() throws Exception {
                                return task.tryCreateTaskDef(project, Lookup.EMPTY);
                            }
                        }).run();
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
            if (project == null) throw new NullPointerException("project");
            if (menu == null) throw new NullPointerException("menu");

            this.project = project;
            this.menu = menu;
            this.lastUsedModel = null;
        }


        public void updateMenuContent() {
            NbGradleModel projectModel = project.getAvailableModel();
            if (lastUsedModel == projectModel) {
                return;
            }

            lastUsedModel = projectModel;

            Collection<NbGradleTask> tasks = projectModel.getMainModule().getTasks();

            menu.removeAll();
            for (final NbGradleTask task: tasks) {
                JMenuItem menuItem = new JMenuItem(task.getLocalName());
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        GradleTasks.createAsyncGradleTask(project, new Callable<GradleTaskDef>() {
                            @Override
                            public GradleTaskDef call() {
                                PredefinedTask predefinedTask
                                        = PredefinedTask.createSimple("", task.getQualifiedName());
                                return predefinedTask.tryCreateTaskDef(project, Lookup.EMPTY);
                            }
                        }).run();
                    }
                });
                menu.add(menuItem);
            }
        }
    }
}
