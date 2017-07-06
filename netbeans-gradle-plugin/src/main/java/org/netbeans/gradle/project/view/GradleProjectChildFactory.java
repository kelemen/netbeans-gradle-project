package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.ManualRefreshedNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.event.PausableChangeListenerManager;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.util.EventUtils;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

public final class GradleProjectChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory>
implements
        RefreshableChildren {

    private static final Logger LOGGER = Logger.getLogger(GradleProjectChildFactory.class.getName());

    private final NbGradleProject project;
    private final GradleProjectLogicalViewProvider parent;
    private final AtomicReference<NodeExtensions> nodeExtensionsRef;
    private final AtomicBoolean lastHasSubprojects;
    private final ListenerRegistrations listenerRefs;
    private final PausableChangeListenerManager refreshNotifier;
    // Nodes comming from the NodeFactory.Registration annotation
    private final AnnotationChildNodes annotationChildNodes;
    private volatile boolean createdOnce;

    public GradleProjectChildFactory(NbGradleProject project, GradleProjectLogicalViewProvider parent) {
        this.project = Objects.requireNonNull(project, "project");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.nodeExtensionsRef = new AtomicReference<>(NodeExtensions.EMPTY);
        this.lastHasSubprojects = new AtomicBoolean(false);
        this.listenerRefs = new ListenerRegistrations();
        this.annotationChildNodes = new AnnotationChildNodes(project);
        this.createdOnce = false;

        this.refreshNotifier = new GenericChangeListenerManager(SwingExecutors.getStrictExecutor(false));
        this.refreshNotifier.registerListener(() -> {
            if (createdOnce) {
                refresh(false);
            }
        });
    }

    private NbGradleModel getShownModule() {
        return project.currentModel().getValue();
    }

    private List<GradleProjectExtensionNodes> getExtensionNodes() {
        List<GradleProjectExtensionNodes> result = new ArrayList<>(
                project.getExtensions().lookupAllExtensionObjs(GradleProjectExtensionNodes.class));
        return result;
    }

    @Override
    public void refreshChildren() {
        refreshNotifier.fireEventually();
    }

    private boolean hasSubProjects() {
        return !getShownModule().getMainProject().getChildren().isEmpty();
    }

    private ListenerRef registerModelRefreshListener() {
        return parent.addChildModelRefreshListener(new ModelRefreshListener() {
            private final AtomicReference<PausableChangeListenerManager.PauseRef> pauseRef
                    = new AtomicReference<>(null);

            @Override
            public void startRefresh() {
                PausableChangeListenerManager.PauseRef prevRef = pauseRef.getAndSet(refreshNotifier.pauseManager());
                if (prevRef != null) {
                    prevRef.unpause();
                }
            }

            @Override
            public void endRefresh(boolean extensionsChanged) {
                if (extensionsChanged) {
                    refreshNotifier.fireEventually();
                }
                PausableChangeListenerManager.PauseRef prevRef = pauseRef.getAndSet(null);
                if (prevRef != null) {
                    prevRef.unpause();
                }
            }
        });
    }

    private boolean tryReplaceNodeExtensionAndClose(NodeExtensions newExtensions) {
        NodeExtensions prevValue;
        do {
            prevValue = nodeExtensionsRef.get();
            if (prevValue == null) {
                if (newExtensions != null) {
                    newExtensions.close();
                }
                return false;
            }
        } while (!nodeExtensionsRef.compareAndSet(prevValue, newExtensions));

        prevValue.close();
        return true;
    }

    private void updateNodesIfNeeded(NodeExtensions newNodeExtensions) {
        if (!tryReplaceNodeExtensionAndClose(newNodeExtensions)) {
            return;
        }

        boolean newHasSubProjects = hasSubProjects();
        boolean prevHasSubProjects = lastHasSubprojects.getAndSet(newHasSubProjects);
        if (newHasSubProjects != prevHasSubProjects) {
            refreshChildren();
        }

        if (newNodeExtensions.isNeedRefreshOnProjectReload()) {
            refreshChildren();
        }
    }

    @Override
    protected void addNotify() {
        Runnable simpleChangeListener = this::refreshChildren;

        annotationChildNodes.addNotify();
        listenerRefs.add(annotationChildNodes.nodeFactories().addChangeListener(simpleChangeListener));

        listenerRefs.add(project.currentModel().addChangeListener(() -> {
            NodeExtensions newNodeExtensions
                    = NodeExtensions.create(getExtensionNodes(), simpleChangeListener);

            updateNodesIfNeeded(newNodeExtensions);
        }));

        listenerRefs.add(EventUtils.asSafeListenerRef(() -> {
            tryReplaceNodeExtensionAndClose(null);
        }));

        listenerRefs.add(registerModelRefreshListener());

        lastHasSubprojects.set(hasSubProjects());

        List<GradleProjectExtensionNodes> extensionNodes = getExtensionNodes();

        for (GradleProjectExtensionNodes singleExtensionNodes: extensionNodes) {
            listenerRefs.add(singleExtensionNodes.addNodeChangeListener(simpleChangeListener));
        }
    }

    @Override
    protected void removeNotify() {
        listenerRefs.unregisterAll();
        annotationChildNodes.removeNotify();
    }

    @Override
    protected Node createNodeForKey(SingleNodeFactory key) {
        return key.createNode();
    }

    private void addProjectFiles(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        toPopulate.add(BuildScriptsNode.getFactory(project));
    }

    private Node createSimpleNode() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        return projectFolder.getNodeDelegate().cloneNode();
    }

    private Children createSubprojectsChild() {
        return Children.create(new SubProjectsChildFactory(project), true);
    }

    private static Action createOpenAction(String caption,
            Collection<? extends NbGradleProjectTree> modules) {
        return OpenProjectsAction.createFromModules(caption, modules);
    }

    private static void getAllChildren(NbGradleProjectTree module, List<NbGradleProjectTree> result) {
        Collection<NbGradleProjectTree> children = module.getChildren();
        result.addAll(children);
        for (NbGradleProjectTree child: children) {
            getAllChildren(child, result);
        }
    }

    public static List<NbGradleProjectTree> getAllChildren(NbGradleProjectTree module) {
        List<NbGradleProjectTree> result = new ArrayList<>();
        getAllChildren(module, result);
        return result;
    }

    private static List<NbGradleProjectTree> getAllChildren(NbGradleModel model) {
        List<NbGradleProjectTree> result = new ArrayList<>();
        getAllChildren(model.getMainProject(), result);
        return result;
    }

    private void addChildren(List<SingleNodeFactory> toPopulate) {
        final NbGradleModel shownModule = getShownModule();
        final Collection<NbGradleProjectTree> immediateChildren
                = shownModule.getMainProject().getChildren();

        if (immediateChildren.isEmpty()) {
            return;
        }
        final List<NbGradleProjectTree> children = getAllChildren(shownModule);

        toPopulate.add(() -> new FilterNode(
                createSimpleNode(),
                createSubprojectsChild(),
                Lookups.singleton(shownModule.getMainProject())) {
                    @Override
                    public String getName() {
                        return "SubProjectsNode_" + getShownModule().getMainProject().getProjectFullName().replace(':', '_');
                    }

                    @Override
                    public Action[] getActions(boolean context) {
                        return new Action[] {
                            createOpenAction(NbStrings.getOpenImmediateSubProjectsCaption(), immediateChildren),
                            createOpenAction(NbStrings.getOpenSubProjectsCaption(), children)
                        };
                    }

                    @Override
                    public String getDisplayName() {
                        return NbStrings.getSubProjectsCaption();
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
                });
    }

    private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        toPopulate.addAll(annotationChildNodes.nodeFactories().getValue());

        List<GradleProjectExtensionNodes> extensionNodes = getExtensionNodes();
        if (extensionNodes != null) {
            for (GradleProjectExtensionNodes nodes: extensionNodes) {
                toPopulate.addAll(nodes.getNodeFactories());
            }
        }

        addChildren(toPopulate);
        addProjectFiles(toPopulate);
    }

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        createdOnce = true;

        try {
            readKeys(toPopulate);
        } catch (DataObjectNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }

    private static boolean isAllAnnotatedWith(
            Collection<?> objects,
            Class<? extends Annotation> annotation) {
        for (Object obj: objects) {
            if (!obj.getClass().isAnnotationPresent(annotation)) {
                return false;
            }
        }
        return true;
    }

    private static final class NodeExtensions {
        private static final NodeExtensions EMPTY = createEmpty();

        private final List<GradleProjectExtensionNodes> nodeFactories;
        private final List<ListenerRef> listenerRefs;
        private final boolean needRefreshOnProjectReload;

        private NodeExtensions(
                Collection<? extends GradleProjectExtensionNodes> nodeFactories,
                List<ListenerRef> listenerRefs) {
            this.nodeFactories = Collections.unmodifiableList(
                    new ArrayList<>(nodeFactories));

            this.listenerRefs = listenerRefs;
            this.needRefreshOnProjectReload = !isAllAnnotatedWith(nodeFactories, ManualRefreshedNodes.class);
        }

        public boolean isNeedRefreshOnProjectReload() {
            return needRefreshOnProjectReload;
        }

        public static NodeExtensions createEmpty() {
            return create(Collections.emptyList(), () -> { });
        }

        public static NodeExtensions create(
                Collection<? extends GradleProjectExtensionNodes> nodeFactories,
                Runnable changeListener) {

            List<ListenerRef> listenerRefs = new ArrayList<>(nodeFactories.size());
            for (GradleProjectExtensionNodes nodeFactory: nodeFactories) {
                listenerRefs.add(nodeFactory.addNodeChangeListener(changeListener));
            }

            NodeExtensions result = new NodeExtensions(nodeFactories, listenerRefs);
            if (result.isNeedRefreshOnProjectReload()) {
                for (GradleProjectExtensionNodes nodeFactory: nodeFactories) {
                    Class<?> nodeFactoryClass = nodeFactory.getClass();
                    if (!nodeFactoryClass.isAnnotationPresent(ManualRefreshedNodes.class)) {
                        LOGGER.log(Level.WARNING,
                                "{0} is not annotated with ManualRefreshedNodes and this will cause project node refresh on all model loads.",
                                nodeFactoryClass.getName());
                    }
                }
            }

            return result;
        }

        public List<GradleProjectExtensionNodes> getFactories() {
            return nodeFactories;
        }

        public void close() {
            listenerRefs.forEach(ListenerRef::unregister);
        }
    }
}
