package org.netbeans.gradle.project.view;

import java.awt.Dialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.netbeans.gradle.project.GradleTasks;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfo.Kind;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleTask;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeEvent;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.ProxyLookup;

public final class GradleProjectLogicalViewProvider implements LogicalViewProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());

        final GradleProjectNode result = new GradleProjectNode(projectFolder.getNodeDelegate().cloneNode());
        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                result.fireInfoChangeEvent();
            }
        };

        project.getProjectInfoManager().addChangeListener(changeListener);
        result.addNodeListener(new NodeAdapter(){
            @Override
            public void nodeDestroyed(NodeEvent ev) {
                project.getProjectInfoManager().removeChangeListener(changeListener);
            }
        });

        return result;
    }

    private Children createChildren() {
        return Children.create(new GradleProjectChildFactory(project), true);
    }

    private Lookup createLookup(Node rootNode) {
        return new ProxyLookup(
                project.getLookup(),
                rootNode.getLookup());
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    private final class GradleProjectNode extends FilterNode {
        private final TasksActionMenu tasksAction;
        private final Action[] actions;

        public GradleProjectNode(Node node) {
            super(node, createChildren(), createLookup(node));

            this.tasksAction = new TasksActionMenu(project);
            this.actions = new Action[] {
                CommonProjectActions.newFileAction(),
                null,
                createProjectAction(
                    ActionProvider.COMMAND_RUN,
                    NbStrings.getRunCommandCaption()),
                createProjectAction(
                    ActionProvider.COMMAND_DEBUG,
                    NbStrings.getDebugCommandCaption()),
                null,
                createProjectAction(
                    ActionProvider.COMMAND_BUILD,
                    NbStrings.getBuildCommandCaption()),
                createProjectAction(
                    ActionProvider.COMMAND_CLEAN,
                    NbStrings.getCleanCommandCaption()),
                createProjectAction(
                    ActionProvider.COMMAND_REBUILD,
                    NbStrings.getRebuildCommandCaption()),
                createProjectAction(
                    GradleActionProvider.COMMAND_JAVADOC,
                    NbStrings.getJavadocCommandCaption()),
                new CustomTaskAction(project),
                this.tasksAction,
                null,
                createProjectAction(
                    GradleActionProvider.COMMAND_RELOAD,
                    NbStrings.getReloadCommandCaption()),
                CommonProjectActions.closeProjectAction(),
                null,
                CommonProjectActions.customizeProjectAction()
            };
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
                appendHtmlList("Error", infoMap.get(ProjectInfo.Kind.ERROR), completeText);
                appendHtmlList("Warning", infoMap.get(ProjectInfo.Kind.WARNING), completeText);
                appendHtmlList("Information", infoMap.get(ProjectInfo.Kind.INFO), completeText);

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
        for (Node child: root.getChildren().getNodes(true)) {
            Node found = PackageView.findPath(child, target);
            if (found != null) {
                return found;
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
        @Override
        public void actionPerformed(ActionEvent e) {
            CustomActionPanel panel = new CustomActionPanel();
            DialogDescriptor dlgDescriptor = new DialogDescriptor(
                    panel,
                    NbStrings.getCustomTaskDlgTitle(),
                    true,
                    DialogDescriptor.OK_CANCEL_OPTION,
                    DialogDescriptor.OK_OPTION,
                    null);
            Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
            dlg.setVisible(true);
            if (DialogDescriptor.OK_OPTION == dlgDescriptor.getValue()) {
                String[] tasks = panel.getTasks();
                String[] args = panel.getArguments();
                String[] jvmArgs = panel.getJvmArguments();

                if (tasks.length > 0) {
                    GradleTasks.createAsyncGradleTask(project, tasks, args, jvmArgs).run();
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
            if (project == null) throw new NullPointerException("project");
            if (menu == null) throw new NullPointerException("menu");

            this.project = project;
            this.menu = menu;
            this.lastUsedModel = null;
        }


        public void updateMenuContent() {
            NbGradleModel projectModel = project.getCurrentModel();
            if (lastUsedModel == projectModel) {
                return;
            }

            if (lastUsedModel != projectModel) {
                lastUsedModel = projectModel;

                Collection<NbGradleTask> tasks = projectModel.getMainModule().getTasks();

                menu.removeAll();
                for (final NbGradleTask task: tasks) {
                    JMenuItem menuItem = new JMenuItem(task.getLocalName());
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            GradleTasks.createAsyncGradleTask(project, task.getQualifiedName()).run();
                        }
                    });
                    menu.add(menuItem);
                }
            }
        }
    }
}
