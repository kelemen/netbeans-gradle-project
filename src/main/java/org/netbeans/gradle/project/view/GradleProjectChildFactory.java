package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.model.GradleProjectInfo;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.Lookups;

public final class GradleProjectChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory> {

    private final NbGradleProject project;
    private final AtomicReference<Runnable> cleanupTaskRef;

    public GradleProjectChildFactory(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.cleanupTaskRef = new AtomicReference<Runnable>(null);
    }

    private NbGradleModel getShownModule() {
        return project.getCurrentModel();
    }

    private List<GradleProjectExtensionNodes> getExtensionNodes() {
        List<GradleProjectExtensionNodes> result = new ArrayList<GradleProjectExtensionNodes>(
                project.getLookup().lookupAll(GradleProjectExtensionNodes.class));
        return result;
    }

    private NbListenerRef addSourcesChangeListener(final Sources sources, final ChangeListener listener) {
        sources.addChangeListener(listener);

        return new NbListenerRef() {
            private AtomicBoolean registered = new AtomicBoolean(true);

            @Override
            public boolean isRegistered() {
                return registered.get();
            }

            @Override
            public void unregister() {
                if (registered.compareAndSet(true, false)) {
                    sources.removeChangeListener(listener);
                }
            }
        };
    }

    private static NbListenerRef noOpListenerRef() {
        return new NbListenerRef() {
            @Override
            public boolean isRegistered() {
                return false;
            }

            @Override
            public void unregister() {
            }
        };
    }

    private NbListenerRef addSourcesChangeListener(final ChangeListener listener) {
        final Lookup.Result<Sources> sourcesResult = project.getLookup().lookupResult(Sources.class);

        final AtomicReference<NbListenerRef> currentListenerRef
                = new AtomicReference<NbListenerRef>(noOpListenerRef());

        final LookupListener lookupListener = new LookupListener() {
            @Override
            public void resultChanged(LookupEvent ev) {
                Lookup lookup = project.getLookup();
                listener.stateChanged(new ChangeEvent(lookup));

                Sources sources = lookup.lookup(Sources.class);
                if (sources != null) {
                    NbListenerRef newListenerRef = addSourcesChangeListener(sources, listener);

                    NbListenerRef prevListener = currentListenerRef.getAndSet(newListenerRef);
                    if (prevListener != null) {
                        prevListener.unregister();
                    }
                    else {
                        currentListenerRef.compareAndSet(newListenerRef, null);
                        newListenerRef.unregister();
                    }
                }
                else {
                    NbListenerRef prevRef = currentListenerRef.getAndSet(noOpListenerRef());
                    if (prevRef == null) {
                        currentListenerRef.set(null);
                    }
                    else {
                        prevRef.unregister();
                    }
                }

                listener.stateChanged(new ChangeEvent(lookup));
            }
        };

        Collection<? extends Sources> sourcesInstances = sourcesResult.allInstances();
        if (!sourcesInstances.isEmpty()) {
            Sources sources = sourcesInstances.iterator().next();
            NbListenerRef listenerRef = addSourcesChangeListener(sources, listener);

            NbListenerRef prevListenerRef = currentListenerRef.getAndSet(listenerRef);
            // No one should have been able to unregister yet.
            prevListenerRef.unregister();
        }

        sourcesResult.addLookupListener(lookupListener);

        return new NbListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                sourcesResult.removeLookupListener(lookupListener);
                NbListenerRef listenerRef = currentListenerRef.getAndSet(null);
                if (listenerRef != null) {
                    listenerRef.unregister();
                }

                registered = false;
            }
        };
    }

    @Override
    protected void addNotify() {
        final Runnable simpleChangeListener = new Runnable() {
            @Override
            public void run() {
                refresh(false);
            }
        };
        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                simpleChangeListener.run();
            }
        };

        List<GradleProjectExtensionNodes> extensionNodes = getExtensionNodes();

        final List<NbListenerRef> listenerRefs = new LinkedList<NbListenerRef>();
        for (GradleProjectExtensionNodes singleExtensionNodes: extensionNodes) {
            listenerRefs.add(singleExtensionNodes.addNodeChangeListener(simpleChangeListener));
        }

        final NbListenerRef sourcesListenerRef = addSourcesChangeListener(changeListener);
        project.addModelChangeListener(changeListener);

        Runnable prevTask = cleanupTaskRef.getAndSet(new Runnable() {
            @Override
            public void run() {
                sourcesListenerRef.unregister();
                project.removeModelChangeListener(changeListener);

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

    private void addSourceGroups(SourceGroup[] groups, List<SingleNodeFactory> toPopulate) {
        for (final SourceGroup group: groups) {
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return PackageView.createPackageView(group);
                }
            });
        }
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

    private Children createSubprojectsChild(List<? extends GradleProjectInfo> children) {
        return Children.create(new SubProjectsChildFactory(project, children), true);
    }

    private static Action createOpenAction(String caption,
            Collection<? extends GradleProjectInfo> modules) {
        return OpenProjectsAction.createFromModules(caption, modules);
    }

    private void addChildren(List<SingleNodeFactory> toPopulate) {
        NbGradleModel shownModule = getShownModule();
        final List<GradleProjectInfo> immediateChildren
                = shownModule.getGradleProjectInfo().getChildren();

        if (immediateChildren.isEmpty()) {
            return;
        }
        final List<GradleProjectInfo> children = NbJavaModelUtils.getAllChildren(shownModule);

        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new FilterNode(
                        createSimpleNode(),
                        createSubprojectsChild(immediateChildren),
                        Lookups.fixed(immediateChildren.toArray())) {
                    @Override
                    public String getName() {
                        return "SubProjectsNode_" + getShownModule().getGradleProject().getPath().replace(':', '_');
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

    private void addSources(List<SingleNodeFactory> toPopulate) {
        Sources sources = ProjectUtils.getSources(project);

        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.SOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.RESOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.TEST_SOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.TEST_RESOURCES), toPopulate);
    }

    private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        addSources(toPopulate);

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
}
