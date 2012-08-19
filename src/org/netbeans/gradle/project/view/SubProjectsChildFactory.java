package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;

public final class SubProjectsChildFactory extends ChildFactory<SingleNodeFactory> {
    private static final Logger LOGGER = Logger.getLogger(SubProjectsChildFactory.class.getName());
    private static final Collator STR_SMP = Collator.getInstance();

    private final NbGradleProject project;
    private final List<NbGradleModule> modules;

    public SubProjectsChildFactory(NbGradleProject project, List<NbGradleModule> modules) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.modules = new ArrayList<NbGradleModule>(modules);
        sortModules(this.modules);

        for (NbGradleModule module: this.modules) {
            if (module == null) throw new NullPointerException("module");
        }
    }

    private static void sortModules(List<NbGradleModule> modules) {
        Collections.sort(modules, new Comparator<NbGradleModule>(){
            @Override
            public int compare(NbGradleModule o1, NbGradleModule o2) {
                return STR_SMP.compare(o1.getName(), o2.getName());
            }
        });
    }

    @Override
    protected Node createNodeForKey(SingleNodeFactory key) {
        return key.createNode();
    }

    private static Lookup createModuleLookup(NbGradleModule module) {
        return Lookups.singleton(module);
    }

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        for (final NbGradleModule module: modules) {
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new SubModuleNode(module);
                }
            });
        }
        return true;
    }

    private static class SubModuleNode extends FilterNode {
        private final NbGradleModule module;
        private final Action[] actions;

        public SubModuleNode(NbGradleModule module) {
            super(Node.EMPTY.cloneNode(), null, createModuleLookup(module));
            this.module = module;

            this.actions = new Action[]{
                new OpenSubProjectAction(module)
            };
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        @Override
        public String getName() {
            return "SubModuleNode_" + module.getUniqueName();
        }
        @Override
        public String getDisplayName() {
            return module.getName();
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
        public boolean canRename() {
            return false;
        }
    }

    @SuppressWarnings("serial") // don't care
    private static class OpenSubProjectAction extends AbstractAction implements Presenter.Popup {
        private final NbGradleModule module;
        private JMenuItem cachedPresenter;

        public OpenSubProjectAction(NbGradleModule module) {
            if (module == null) throw new NullPointerException("module");
            this.module = module;
            this.cachedPresenter = null;
        }

        public JMenuItem createPresenter() {
            JMenuItem presenter = new JMenuItem(NbStrings.getOpenSubProjectCaption());
            presenter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openSubProject();
                }
            });
            return presenter;
        }

        @Override
        public JMenuItem getPopupPresenter() {
            if (cachedPresenter == null) {
                cachedPresenter = createPresenter();
            }
            return cachedPresenter;
        }

        private void openSubProject() {
            LOGGER.log(Level.FINE, "Trying to open subproject: {0}", module.getName());
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    FileObject subProjectDir = FileUtil.toFileObject(module.getModuleDir());
                    if (subProjectDir == null) {
                        LOGGER.log(Level.WARNING, "Directory of the subproject does not exist: {0}", module.getModuleDir());
                        return;
                    }

                    try {
                        ProjectManager projectManager = ProjectManager.getDefault();

                        Closeable safeToOpenKey = NbGradleProjectFactory.safeToOpen(subProjectDir);
                        try {
                            // We have to clear this list because if the project
                            // does not have build.gradle, NetBeans might have
                            // already determined that the directory does not
                            // contain a project.
                            projectManager.clearNonProjectCache();

                            Project subProject = projectManager.findProject(subProjectDir);
                            if (subProject == null) {
                                LOGGER.log(Level.WARNING,
                                        "Subproject cannot be found: {0}",
                                        module.getModuleDir());
                                return;
                            }
                            OpenProjects.getDefault().open(new Project[]{subProject}, false);

                        } finally {
                            safeToOpenKey.close();
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "Error while trying to load the project: " + module.getModuleDir(),
                                ex);
                    }
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Action is done by the menu item.
        }
    }
}
