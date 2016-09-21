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
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.NbBiFunction;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.gradle.project.util.NbSupplier;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.Lookups;

public final class AnnotationChildNodes {
    private static final Logger LOGGER = Logger.getLogger(AnnotationChildNodes.class.getName());

    private static final PropertySource<Collection<SingleNodeFactory>> NO_FACTORIES
            = PropertyFactory.<Collection<SingleNodeFactory>>constSource(Collections.<SingleNodeFactory>emptySet());

    private final Project project;
    private final RemovedChildrenProperty removeChildrenRef;
    private final PropertySource<Collection<? extends NodeFactory>> nodeFactories;
    private final PropertySource<Collection<SingleNodeFactory>> singleNodeFactories;

    private final Lock nodeLock;
    private boolean removedChildren;
    private Set<NodeList<?>> currentNodeLists;

    public AnnotationChildNodes(Project project) {
        this(project, new NbSupplier<Lookup>() {
            @Override
            public Lookup get() {
                return Lookups.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Nodes");
            }
        });
    }

    public AnnotationChildNodes(Project project, NbSupplier<? extends Lookup> factoryLookupProvider) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(factoryLookupProvider, "factoryLookupProvider");

        this.project = project;
        this.nodeLock = new ReentrantLock();
        this.removedChildren = true;
        this.removeChildrenRef = new RemovedChildrenProperty();
        this.currentNodeLists = Collections.emptySet();

        this.nodeFactories = NbProperties.combine(new NodeFactories(factoryLookupProvider), this.removeChildrenRef, new NbBiFunction<Collection<? extends NodeFactory>, Boolean, Collection<? extends NodeFactory>>() {
            @Override
            public Collection<? extends NodeFactory> apply(Collection<? extends NodeFactory> factories, Boolean removedChildren) {
                return removedChildren ? null : factories;
            }
        });
        this.singleNodeFactories = NbProperties.propertyOfProperty(nodeFactories, new NbFunction<Collection<? extends NodeFactory>, PropertySource<Collection<SingleNodeFactory>>>() {
            @Override
            public PropertySource<Collection<SingleNodeFactory>> apply(Collection<? extends NodeFactory> arg) {
                return convertFactories(arg);
            }
        });
    }

    private void createAll(
            Collection<? extends NodeFactory> factories,
            Collection<NodeList<?>> result) {

        for (NodeFactory factory: factories) {
            try {
                NodeList<?> nodeList = factory.createNodes(project);
                result.add(nodeList);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Exception thrown by NodeFactory.createNodes: " + factory, ex);
            }
        }
    }

    private Collection<NodeList<?>> updateAndGetNodeLists(Collection<? extends NodeFactory> factories) {
        if (factories == null) {
            return Collections.emptyList();
        }

        Collection<NodeList<?>> result = CollectionUtils.newLinkedHashSet(factories.size());
        createAll(factories, result);

        List<NodeList<?>> removed = new ArrayList<>();
        List<NodeList<?>> added = new ArrayList<>();

        nodeLock.lock();
        try {
            if (removedChildren) {
                assert currentNodeLists.isEmpty();
                return Collections.emptyList();
            }

            Set<NodeList<?>> prevNodeLists = currentNodeLists;
            currentNodeLists = new LinkedHashSet<>(result);

            for (NodeList<?> nodeList: result) {
                if (!prevNodeLists.contains(nodeList)) {
                    added.add(nodeList);
                }
            }
            for (NodeList<?> nodeList: prevNodeLists) {
                if (!result.contains(nodeList)) {
                    removed.add(nodeList);
                }
            }
        } finally {
            nodeLock.unlock();
        }

        removeNotifyAll(removed);
        addNotifyAll(added);

        return result;
    }

    private PropertySource<Collection<SingleNodeFactory>> convertFactories(Collection<? extends NodeFactory> factories) {
        final Collection<NodeList<?>> nodeLists = updateAndGetNodeLists(factories);
        if (nodeLists.isEmpty()) {
            return NO_FACTORIES;
        }

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

        final UpdateTaskExecutor listenerExecutor = new SwingUpdateTaskExecutor(false);
        return SwingProperties.fromSwingSource(result, new SwingForwarderFactory<ChangeListener>() {
            @Override
            public ChangeListener createForwarder(final Runnable listener) {
                return new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        listenerExecutor.execute(listener);
                    }
                };
            }
        });
    }

    private static <T> void addNodeListNodes(NodeList<T> nodeList, List<SingleNodeFactory> toPopulate) {
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
        removeChildrenRef.fireOnChange();
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

        removeChildrenRef.fireOnChange();
        removeNotifyAll(removed);
    }

    public PropertySource<Collection<SingleNodeFactory>> nodeFactories() {
        return singleNodeFactories;
    }

    private class RemovedChildrenProperty implements PropertySource<Boolean> {
        private final ListenerManager<Runnable> changeListeners;
        private final EventDispatcher<Runnable, Void> listenerDispatcher;

        public RemovedChildrenProperty() {
            this.changeListeners = new CopyOnTriggerListenerManager<>();

            final UpdateTaskExecutor listenerExecutor = new SwingUpdateTaskExecutor(false);
            this.listenerDispatcher = new EventDispatcher<Runnable, Void>() {
                @Override
                public void onEvent(Runnable eventListener, Void arg) {
                    listenerExecutor.execute(eventListener);
                }
            };
        }

        public void fireOnChange() {
            changeListeners.onEvent(listenerDispatcher, null);
        }

        @Override
        public Boolean getValue() {
            nodeLock.lock();
            try {
                return removedChildren;
            } finally {
                nodeLock.unlock();
            }
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return changeListeners.registerListener(listener);
        }
    }

    private static class NodeFactories implements PropertySource<Collection<? extends NodeFactory>> {
        private final AtomicReference<Lookup.Result<NodeFactory>> nodeListsRef;
        private final NbSupplier<? extends Lookup> factoryLookupProvider;
        private final UpdateTaskExecutor listenerExecutor;

        public NodeFactories(NbSupplier<? extends Lookup> factoryLookupProvider) {
            assert factoryLookupProvider != null;

            this.factoryLookupProvider = factoryLookupProvider;
            this.nodeListsRef = new AtomicReference<>(null);
            this.listenerExecutor = new SwingUpdateTaskExecutor(false);
        }

        private Lookup.Result<NodeFactory> getNodeListsResult() {
            Lookup.Result<NodeFactory> result = nodeListsRef.get();
            if (result == null) {
                Lookup nodeListsLookup = factoryLookupProvider.get();
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
                    listenerExecutor.execute(listener);
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
            return 355 + Objects.hashCode(key);
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
