package org.netbeans.gradle.project.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.Lookups;

public final class AnnotationChildNodes {
    private static final Logger LOGGER = Logger.getLogger(AnnotationChildNodes.class.getName());

    private final Project project;
    private final PropertySource<Collection<? extends NodeFactory>> nodeFactories;
    private final PropertySource<Collection<SingleNodeFactory>> singleNodeFactories;

    private final Lock nodeLock;
    private boolean removedChildren;
    private Set<NodeList<?>> currentNodeLists;

    public AnnotationChildNodes(Project project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;
        this.nodeLock = new ReentrantLock();
        this.removedChildren = false;
        this.currentNodeLists = Collections.emptySet();
        this.nodeFactories = new NodeFactories();
        this.singleNodeFactories = NbProperties.propertyOfProperty(nodeFactories, new NbFunction<Collection<? extends NodeFactory>, PropertySource<Collection<SingleNodeFactory>>>() {
            @Override
            public PropertySource<Collection<SingleNodeFactory>> apply(Collection<? extends NodeFactory> arg) {
                return convertFactories(arg);
            }
        });
    }

    private PropertySource<Collection<SingleNodeFactory>> convertFactories(Collection<? extends NodeFactory> factories) {
        final List<NodeList<?>> nodeLists = new ArrayList<>(factories.size());
        for (NodeFactory factory: factories) {
            try {
                NodeList<?> nodeList = factory.createNodes(project);
                nodeLists.add(nodeList);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Exception thrown by NodeFactory.createNodes: " + factory, ex);
            }
        }

        List<NodeList<?>> removed = new ArrayList<>();
        List<NodeList<?>> added = new ArrayList<>();

        nodeLock.lock();
        try {
            if (removedChildren) {
                nodeLists.clear();
            }

            Set<NodeList<?>> prevNodeLists = currentNodeLists;
            currentNodeLists = new LinkedHashSet<>(nodeLists);

            for (NodeList<?> nodeList: nodeLists) {
                if (!prevNodeLists.contains(nodeList)) {
                    added.add(nodeList);
                }
            }
            for (NodeList<?> nodeList: prevNodeLists) {
                if (!nodeLists.contains(nodeList)) {
                    removed.add(nodeList);
                }
            }
        } finally {
            nodeLock.unlock();
        }

        removeNotifyAll(removed);
        addNotifyAll(added);

        SwingPropertySource<Collection<SingleNodeFactory>, ChangeListener> result = new SwingPropertySource<Collection<SingleNodeFactory>, ChangeListener>() {
            @Override
            public Collection<SingleNodeFactory> getValue() {
                List<SingleNodeFactory> nodeFactories = new ArrayList<>();
                for (NodeList<?> nodeList: nodeLists) {
                    addNodeListNodes(nodeList, nodeFactories);
                }
                return nodeFactories;
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                for (NodeList<?> nodeList: nodeLists) {
                    nodeList.addChangeListener(listener);
                }
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                for (NodeList<?> nodeList: nodeLists) {
                    nodeList.removeChangeListener(listener);
                }
            }
        };

        return SwingProperties.fromSwingSource(result, new SwingForwarderFactory<ChangeListener>() {
            @Override
            public ChangeListener createForwarder(final Runnable listener) {
                return new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        listener.run();
                    }
                };
            }
        });
    }

    private <T> void addNodeListNodes(NodeList<T> nodeList, List<SingleNodeFactory> toPopulate) {
        for (T key: nodeList.keys()) {
            toPopulate.add(new NodeListNodeFactory<>(nodeList, key));
        }
    }

    private static void addNotifyAll(Collection<NodeList<?>> nodeLists) {
        for (NodeList<?> nodeList: nodeLists) {
            try {
                nodeList.addNotify();
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Exception thrown by NodeList.addNotify: " + nodeList, ex);
            }
        }
    }

    private static void removeNotifyAll(Collection<NodeList<?>> nodeLists) {
        for (NodeList<?> nodeList: nodeLists) {
            try {
                nodeList.removeNotify();
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Exception thrown by NodeList.removeNotify: " + nodeList, ex);
            }
        }
    }

    public void addNotify() {
        nodeLock.lock();
        try {
            removedChildren = false;
        } finally {
            nodeLock.unlock();
        }
    }

    public void removeNotify() {
        List<NodeList<?>> removed;
        nodeLock.lock();
        try {
            removedChildren = true;
            removed = new ArrayList<>(currentNodeLists);
            currentNodeLists = Collections.emptySet();
        } finally {
            nodeLock.unlock();
        }

        removeNotifyAll(removed);
    }

    public PropertySource<Collection<SingleNodeFactory>> nodeFactories() {
        return singleNodeFactories;
    }

    private static class NodeFactories implements PropertySource<Collection<? extends NodeFactory>> {
        private final AtomicReference<Lookup.Result<NodeFactory>> nodeListsRef;

        public NodeFactories() {
            this.nodeListsRef = new AtomicReference<>(null);
        }

        private Lookup.Result<NodeFactory> getNodeListsResult() {
            Lookup.Result<NodeFactory> result = nodeListsRef.get();
            if (result == null) {
                Lookup nodeListsLookup = Lookups.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Nodes");
                result = nodeListsLookup.lookupResult(NodeFactory.class);
                if (!nodeListsRef.compareAndSet(null, result)) {
                    result = nodeListsRef.get();
                }
            }
            return result;
        }

        @Override
        public Collection<? extends NodeFactory> getValue() {
            return getNodeListsResult().allInstances();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            final LookupListener wrapper = new LookupListener() {
                @Override
                public void resultChanged(LookupEvent ev) {
                    listener.run();
                }
            };

            final Lookup.Result<NodeFactory> nodeLists = getNodeListsResult();
            nodeLists.addLookupListener(wrapper);
            return NbListenerRefs.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    nodeLists.removeLookupListener(wrapper);
                }
            });
        }
    }

    private static final class NodeListNodeFactory<T> implements SingleNodeFactory {
        private final NodeList<? super T> nodeList;
        private final T key;

        public NodeListNodeFactory(NodeList<? super T> nodeList, T key) {
            this.nodeList = nodeList;
            this.key = key;
        }

        @Override
        public Node createNode() {
            return nodeList.node(key);
        }

        @Override
        public int hashCode() {
            return  355 + Objects.hashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final NodeListNodeFactory<?> other = (NodeListNodeFactory<?>)obj;
            return Objects.equals(this.key, other.key);
        }
    }
}
