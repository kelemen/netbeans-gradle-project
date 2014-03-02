package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.openide.loaders.DataFolder;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SubProjectsChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory>
implements
        ChangeListener {

    private static final Logger LOGGER = Logger.getLogger(SubProjectsChildFactory.class.getName());
    private static final Collator STR_SMP = Collator.getInstance();

    private final NbGradleProject project;
    private final List<NbGradleProjectTree> subProjects;
    private final AtomicReference<NbGradleProjectTree> lastTree;
    private final boolean root;

    public SubProjectsChildFactory(NbGradleProject project) {
        this(project, null, true);
    }

    private SubProjectsChildFactory(
            NbGradleProject project,
            Collection<? extends NbGradleProjectTree> subProjects,
            boolean root) {

        ExceptionHelper.checkNotNullArgument(project, "project");

        this.root = root;
        this.project = project;
        this.lastTree = new AtomicReference<>(null);
        if (subProjects != null) {
            this.subProjects = new ArrayList<>(subProjects);
            sortModules(this.subProjects);

            ExceptionHelper.checkNotNullElements(this.subProjects, "subProjects");
        }
        else {
            this.subProjects = null;
        }
    }

    private static void sortModules(List<NbGradleProjectTree> modules) {
        Collections.sort(modules, new Comparator<NbGradleProjectTree>(){
            @Override
            public int compare(NbGradleProjectTree o1, NbGradleProjectTree o2) {
                return STR_SMP.compare(o1.getProjectName(), o2.getProjectName());
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        NbGradleProjectTree newTree = project.getAvailableModel().getMainProject();
        NbGradleProjectTree prevTree = lastTree.getAndSet(newTree);
        if (hasRelevantDifferences(prevTree, newTree)) {
            refresh(false);
        }
    }

    private static boolean hasRelevantDifferences(NbGradleProjectTree tree1, NbGradleProjectTree tree2) {
        if (tree1 == tree2) {
            // In practice this happens only when they are nulls.
            return false;
        }
        if (tree1 == null || tree2 == null) {
            return true;
        }

        return !equalsTree(tree1, tree2);
    }

    private static boolean equalsTree(NbGradleProjectTree tree1, NbGradleProjectTree tree2) {
        Collection<NbGradleProjectTree> children1 = tree1.getChildren();
        Collection<NbGradleProjectTree> children2 = tree2.getChildren();
        if (children1.size() != children2.size()) {
            return false;
        }
        if (children1.isEmpty()) {
            return true;
        }

        Map<String, NbGradleProjectTree> children2Map = getChildrenMap2(tree2);
        for (NbGradleProjectTree subTree1: children1) {
            NbGradleProjectTree subTree2 = children2Map.get(subTree1.getProjectName());
            if (subTree2 == null) {
                return false;
            }

            if (!equalsTree(subTree1, subTree2)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, NbGradleProjectTree> getChildrenMap2(NbGradleProjectTree tree) {
        Collection<NbGradleProjectTree> children = tree.getChildren();
        Map<String, NbGradleProjectTree> result = CollectionUtils.newHashMap(children.size());

        for (NbGradleProjectTree child: children) {
            result.put(child.getProjectName(), child);
        }
        return result;
    }

    @Override
    protected void addNotify() {
        if (root) {
            project.addModelChangeListener(this);
        }
    }

    @Override
    protected void removeNotify() {
        if (root) {
            project.removeModelChangeListener(this);
        }
    }

    @Override
    protected Node createNodeForKey(SingleNodeFactory key) {
        return key.createNode();
    }

    private List<NbGradleProjectTree> getSubProjects() {
        if (subProjects != null) {
            return subProjects;
        }

        List<NbGradleProjectTree> result = new ArrayList<>(
                project.getAvailableModel().getMainProject().getChildren());
        sortModules(result);
        return result;
    }

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        for (final NbGradleProjectTree subProject: getSubProjects()) {
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    if (subProject.getChildren().isEmpty()) {
                        return new SubModuleNode(project, subProject);
                    }
                    else {
                        return new SubModuleWithChildren(project, subProject);
                    }
                }
            });
        }
        return true;
    }

    private static Children createSubprojectsChild(
            NbGradleProject project,
            Collection<? extends NbGradleProjectTree> children) {

        return Children.create(new SubProjectsChildFactory(project, children, false), true);
    }

    private static Node createSimpleNode(NbGradleProject project) {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        return projectFolder.getNodeDelegate().cloneNode();
    }

    private static Action createOpenAction(String caption,
            Collection<NbGradleProjectTree> projects) {
        return OpenProjectsAction.createFromModules(caption, projects);
    }

    private static class SubModuleWithChildren extends FilterNode {
        private final NbGradleProjectTree module;
        private final List<NbGradleProjectTree> immediateChildren;
        private final List<NbGradleProjectTree> children;

        public SubModuleWithChildren(NbGradleProject project, NbGradleProjectTree module) {
            this(project, module, module.getChildren());
        }

        private SubModuleWithChildren(
                NbGradleProject project,
                NbGradleProjectTree module,
                Collection<? extends NbGradleProjectTree> children) {

            super(createSimpleNode(project),
                    createSubprojectsChild(project, children),
                    Lookups.fixed(module));
            this.module = module;
            this.immediateChildren = Collections.unmodifiableList(GradleProjectChildFactory.getAllChildren(module));
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }

        @Override
        public String getName() {
            return "SubProjectsNode_" + module.getProjectFullName().replace(':', '_');
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[] {
                new OpenSubProjectAction(),
                createOpenAction(NbStrings.getOpenImmediateSubProjectsCaption(), immediateChildren),
                createOpenAction(NbStrings.getOpenSubProjectsCaption(), children)
            };
        }

        @Override
        public Action getPreferredAction() {
            return new OpenSubProjectAction();
        }

        @Override
        public String getDisplayName() {
            return module.getProjectName();
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
        private final NbGradleProjectTree module;

        public SubModuleNode(NbGradleProject project, NbGradleProjectTree module) {
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
            return "SubModuleNode_" + module.getProjectFullName().replace(':', '_');
        }
        @Override
        public String getDisplayName() {
            return module.getProjectName();
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
            final Collection<? extends NbGradleProjectTree> projects
                    = actionContext.lookupAll(NbGradleProjectTree.class);

            return createOpenAction(
                    NbStrings.getOpenSubProjectCaption(projects),
                    Collections.unmodifiableCollection(projects));
        }
    }
}
