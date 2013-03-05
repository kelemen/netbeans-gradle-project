package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.openide.loaders.DataFolder;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
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

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        for (final NbGradleModule module: modules) {
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    if (module.getChildren().isEmpty()) {
                        return new SubModuleNode(project, module);
                    }
                    else {
                        return new SubModuleWithChildren(project, module);
                    }
                }
            });
        }
        return true;
    }

    private static Children createSubprojectsChild(NbGradleProject project, List<NbGradleModule> children) {
        return Children.create(new SubProjectsChildFactory(project, children), true);
    }

    private static Node createSimpleNode(NbGradleProject project) {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        return projectFolder.getNodeDelegate().cloneNode();
    }

    private static Action createOpenAction(String caption,
            Collection<NbGradleModule> projects) {
        return OpenProjectsAction.createFromModules(caption, projects);
    }

    private static class SubModuleWithChildren extends FilterNode {
        private final NbGradleModule module;
        private final List<NbGradleModule> immdeiateChildren;
        private final List<NbGradleModule> children;

        public SubModuleWithChildren(NbGradleProject project, NbGradleModule module) {
            this(project, module, module.getChildren());
        }

        private SubModuleWithChildren(NbGradleProject project, NbGradleModule module, List<NbGradleModule> children) {
            super(createSimpleNode(project),
                    createSubprojectsChild(project, children),
                    Lookups.fixed(module));
            this.module = module;
            this.immdeiateChildren = Collections.unmodifiableList(NbModelUtils.getAllChildren(module));
            this.children = Collections.unmodifiableList(new ArrayList<NbGradleModule>(children));
        }

        @Override
        public String getName() {
            return "SubProjectsNode_" + module.getUniqueName();
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[] {
                new OpenSubProjectAction(),
                createOpenAction(NbStrings.getOpenImmediateSubProjectsCaption(), immdeiateChildren),
                createOpenAction(NbStrings.getOpenSubProjectsCaption(), children)
            };
        }

        @Override
        public Action getPreferredAction() {
            return new OpenSubProjectAction();
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

    private static class SubModuleNode extends FilterNode {
        private final NbGradleModule module;

        public SubModuleNode(NbGradleProject project, NbGradleModule module) {
            super(Node.EMPTY.cloneNode(), null, Lookups.fixed(project, module));
            this.module = module;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{
                new OpenSubProjectAction()
            };
        }

        @Override
        public Action getPreferredAction() {
            return new OpenSubProjectAction();
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
    private static class OpenSubProjectAction
    extends
            AbstractAction
    implements
            ContextAwareAction {

        @Override
        public int hashCode() {
            return 5 * getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            return getClass() == obj.getClass();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LOGGER.warning("SubProjectsChildFactory.OpenSubProjectAction.actionPerformed has been invoked.");
        }

        @Override
        public Action createContextAwareInstance(Lookup actionContext) {
            final Collection<? extends NbGradleModule> projects
                    = actionContext.lookupAll(NbGradleModule.class);

            return createOpenAction(
                    NbStrings.getOpenSubProjectCaption(projects),
                    Collections.unmodifiableCollection(projects));
        }
    }
}
