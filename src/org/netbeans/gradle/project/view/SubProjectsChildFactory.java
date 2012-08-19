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
import org.netbeans.gradle.project.model.NbGradleModule;
import org.openide.nodes.ChildFactory;
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
                    return new SubModuleNode(project, module);
                }
            });
        }
        return true;
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

            return new OpenProjectsAction(Collections.unmodifiableCollection(projects));
        }
    }
}
