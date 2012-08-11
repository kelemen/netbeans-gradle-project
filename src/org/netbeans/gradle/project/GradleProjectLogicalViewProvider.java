package org.netbeans.gradle.project;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
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
        return new GradleProjectNode(projectFolder.getNodeDelegate());
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
                    ActionProvider.COMMAND_BUILD,
                    NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_Build")),
                createProjectAction(
                    ActionProvider.COMMAND_CLEAN,
                    NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_Clean")),
                createProjectAction(
                    ActionProvider.COMMAND_REBUILD,
                    NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_Rebuild")),
                createProjectAction(
                    GradleActionProvider.COMMAND_JAVADOC,
                    NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_Javadoc")),
                this.tasksAction,
                null,
                createProjectAction(
                    GradleActionProvider.COMMAND_RELOAD,
                    NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_ReloadProject")),
                CommonProjectActions.closeProjectAction(),
            };
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        @Override
        public Image getIcon(int type) {
            return NbIcons.getGradleIcon();
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
        //leave unimplemented for now
        return null;
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
            JMenu menu = new JMenu(NbBundle.getMessage(GradleProjectLogicalViewProvider.class, "LBL_Tasks"));
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

                Collection<String> tasks = projectModel.getMainModule().getTasks();

                menu.removeAll();
                for (final String task: tasks) {
                    JMenuItem menuItem = new JMenuItem(task);
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            GradleTasks.createAsyncGradleTask(project, task).run();
                        }
                    });
                    menu.add(menuItem);
                }
            }
        }
    }
}
