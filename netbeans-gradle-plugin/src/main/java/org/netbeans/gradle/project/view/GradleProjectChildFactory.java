package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

public final class GradleProjectChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory> {

    private final NbGradleProject project;
    private final AtomicReference<Runnable> cleanupTaskRef;
    private final AtomicReference<NodeExtensions> nodeExtensionsRef;

    public GradleProjectChildFactory(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.cleanupTaskRef = new AtomicReference<Runnable>(null);
        this.nodeExtensionsRef = new AtomicReference<NodeExtensions>(NodeExtensions.EMPTY);
    }

    private NbGradleModel getShownModule() {
        return project.getCurrentModel();
    }

    private List<GradleProjectExtensionNodes> getExtensionNodes() {
        List<GradleProjectExtensionNodes> result = new ArrayList<GradleProjectExtensionNodes>(
                project.getCombinedExtensionLookup().lookupAll(GradleProjectExtensionNodes.class));
        return result;
    }

    @Override
    protected void addNotify() {
        final Runnable simpleChangeListener = new Runnable() {
            @Override
            public void run() {
                refresh(false);
            }
        };

        List<GradleProjectExtensionNodes> extensionNodes = getExtensionNodes();

        final List<NbListenerRef> listenerRefs = new LinkedList<NbListenerRef>();
        for (GradleProjectExtensionNodes singleExtensionNodes: extensionNodes) {
            listenerRefs.add(singleExtensionNodes.addNodeChangeListener(simpleChangeListener));
        }

        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                NodeExtensions newNodeExtensions
                        = NodeExtensions.create(getExtensionNodes(), simpleChangeListener);

                // FIXME: This is not trully thread safe because model change
                //   listener might be called after removeNotify, leaving an
                //   unclosed NodeExtensions instance.
                NodeExtensions prevNodeExtensions = nodeExtensionsRef.getAndSet(newNodeExtensions);
                prevNodeExtensions.close();

                simpleChangeListener.run();
            }
        };
        project.addModelChangeListener(changeListener);

        Runnable prevTask = cleanupTaskRef.getAndSet(new Runnable() {
            @Override
            public void run() {
                project.removeModelChangeListener(changeListener);

                nodeExtensionsRef.getAndSet(NodeExtensions.EMPTY).close();

                for (NbListenerRef ref: listenerRefs) {
                    ref.unregister();
                }
            }
        });
        if (prevTask != null) {
            throw new IllegalStateException("addNotify has been called multiple times.");
        }
    }

    @Override
    protected void removeNotify() {
        Runnable cleanupTask = cleanupTaskRef.getAndSet(null);
        if (cleanupTask != null) {
            cleanupTask.run();
        }
    }

    @Override
    protected Node createNodeForKey(SingleNodeFactory key) {
        return key.createNode();
    }

    private void addProjectFiles(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new BuildScriptsNode(project);
            }
        });
    }

    private Node createSimpleNode() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        return projectFolder.getNodeDelegate().cloneNode();
    }

    private Children createSubprojectsChild(Collection<? extends NbGradleProjectTree> children) {
        return Children.create(new SubProjectsChildFactory(project, children), true);
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
        List<NbGradleProjectTree> result = new LinkedList<NbGradleProjectTree>();
        getAllChildren(module, result);
        return result;
    }

    private static List<NbGradleProjectTree> getAllChildren(NbGradleModel model) {
        List<NbGradleProjectTree> result = new LinkedList<NbGradleProjectTree>();
        getAllChildren(model.getMainProject(), result);
        return result;
    }

    private void addChildren(List<SingleNodeFactory> toPopulate) {
        NbGradleModel shownModule = getShownModule();
        final Collection<NbGradleProjectTree> immediateChildren
                = shownModule.getMainProject().getChildren();

        if (immediateChildren.isEmpty()) {
            return;
        }
        final List<NbGradleProjectTree> children = getAllChildren(shownModule);

        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new FilterNode(
                        createSimpleNode(),
                        createSubprojectsChild(immediateChildren),
                        Lookups.fixed(immediateChildren.toArray())) {
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
                };
            }
        });
    }

    private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
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
        try {
            readKeys(toPopulate);
        } catch (DataObjectNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }

    private static final class NodeExtensions {
        private static final NodeExtensions EMPTY = createEmpty();

        private final List<GradleProjectExtensionNodes> nodeFactories;
        private final List<NbListenerRef> listenerRefs;

        private NodeExtensions(
                Collection<? extends GradleProjectExtensionNodes> nodeFactories,
                List<NbListenerRef> listenerRefs) {
            this.nodeFactories = Collections.unmodifiableList(
                    new ArrayList<GradleProjectExtensionNodes>(nodeFactories));

            this.listenerRefs = listenerRefs;
        }

        public static NodeExtensions createEmpty() {
            return create(Collections.<GradleProjectExtensionNodes>emptyList(), new Runnable() {
                @Override
                public void run() {
                    // Do nothing.
                }
            });
        }

        public static NodeExtensions create(
                Collection<? extends GradleProjectExtensionNodes> nodeFactories,
                Runnable changeListener) {

            List<NbListenerRef> listenerRefs = new ArrayList<NbListenerRef>(nodeFactories.size());
            for (GradleProjectExtensionNodes nodeFactory: nodeFactories) {
                listenerRefs.add(nodeFactory.addNodeChangeListener(changeListener));
            }

            return new NodeExtensions(nodeFactories, listenerRefs);
        }

        public List<GradleProjectExtensionNodes> getFactories() {
            return nodeFactories;
        }

        public void close() {
            for (NbListenerRef ref: listenerRefs) {
                ref.unregister();
            }
        }
    }
}
