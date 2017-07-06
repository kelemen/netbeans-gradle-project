package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.StringUtils;
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
        ChildFactory.Detachable<SingleNodeFactory> {

    private static final Logger LOGGER = Logger.getLogger(SubProjectsChildFactory.class.getName());

    private final NbGradleProject project;
    private final List<NbGradleProjectTree> subProjects;
    private final AtomicReference<NbGradleProjectTree> lastTree;
    private final boolean root;
    private final ListenerRegistrations listenerRefs;

    public SubProjectsChildFactory(NbGradleProject project) {
        this(project, null, true);
    }

    private SubProjectsChildFactory(
            NbGradleProject project,
            Collection<? extends NbGradleProjectTree> subProjects,
            boolean root) {

        this.root = root;
        this.project = Objects.requireNonNull(project, "project");
        this.listenerRefs = new ListenerRegistrations();
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
        modules.sort(Comparator.comparing(NbGradleProjectTree::getProjectName, StringUtils.STR_CMP::compare));
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

    private void modelChanged() {
        NbGradleProjectTree newTree = project.currentModel().getValue().getMainProject();
        NbGradleProjectTree prevTree = lastTree.getAndSet(newTree);
        if (hasRelevantDifferences(prevTree, newTree)) {
            refresh(false);
        }
    }

    @Override
    protected void addNotify() {
        if (root) {
            listenerRefs.add(project.currentModel().addChangeListener(this::modelChanged));
        }
    }

    @Override
    protected void removeNotify() {
        listenerRefs.unregisterAll();
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
                project.currentModel().getValue().getMainProject().getChildren());
        sortModules(result);
        return result;
    }

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        for (NbGradleProjectTree subProject: getSubProjects()) {
            toPopulate.add(() -> {
                if (subProject.getChildren().isEmpty()) {
                    return new SubModuleNode(project, subProject);
                }
                else {
                    return new SubModuleWithChildren(project, subProject);
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

    private static Action[] getSubProjectContextActions(NbGradleProjectTree module, Action... defaultActions) {
        Project project = NbGradleProjectFactory.tryLoadSafeProject(module.getProjectDir());
        if (project == null) {
            return defaultActions;
        }

        return getSubProjectContextActions(project, defaultActions);
    }

    private static Action[] getSubProjectContextActions(Project project, Action... defaultActions) {
        ContextActionProvider actionProvider = project.getLookup().lookup(ContextActionProvider.class);
        if (actionProvider == null) {
            return defaultActions;
        }

        Action[] projectActions = actionProvider.getActions();

        Action[] result = new Action[defaultActions.length + projectActions.length + 1];
        System.arraycopy(defaultActions, 0, result, 0, defaultActions.length);
        result[defaultActions.length] = null;
        System.arraycopy(projectActions, 0, result, defaultActions.length + 1, projectActions.length);

        return result;
    }

    private static Lookup getSubProjectLookup(NbGradleProjectTree module) {
        Project project = NbGradleProjectFactory.tryLoadSafeProject(module.getProjectDir());
        if (project == null) {
            return Lookups.fixed(module);
        }
        else {
            return Lookups.fixed(module, project);
        }
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
                    getSubProjectLookup(module));
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
            return getSubProjectContextActions(module,
                    new OpenSubProjectAction(),
                    createOpenAction(NbStrings.getOpenImmediateSubProjectsCaption(), immediateChildren),
                    createOpenAction(NbStrings.getOpenSubProjectsCaption(), children));
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
            super(Node.EMPTY.cloneNode(), null, getSubProjectLookup(module));
            this.module = module;
        }

        @Override
        public Action[] getActions(boolean context) {
            return getSubProjectContextActions(module, new OpenSubProjectAction());
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
