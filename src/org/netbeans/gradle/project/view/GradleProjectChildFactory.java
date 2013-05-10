package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.event.ListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleProjectChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory> {

    private final NbGradleProject project;
    private final AtomicReference<Runnable> cleanupTaskRef;
    private volatile List<GradleProjectExtensionNodes> currentExtensions;

    public GradleProjectChildFactory(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.cleanupTaskRef = new AtomicReference<Runnable>(null);
        this.currentExtensions = null;
    }

    private NbGradleModule getShownModule() {
        return project.getCurrentModel().getMainModule();
    }

    private List<GradleProjectExtensionNodes> getExtensionNodes() {
        List<GradleProjectExtensionNodes> result = new LinkedList<GradleProjectExtensionNodes>();
        for (GradleProjectExtension extension: project.getExtensions()) {
            Lookup extensionLookup = extension.getExtensionLookup();
            result.addAll(extensionLookup.lookupAll(GradleProjectExtensionNodes.class));
        }
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
        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                simpleChangeListener.run();
            }
        };

        List<GradleProjectExtensionNodes> extensionNodes = getExtensionNodes();
        currentExtensions = extensionNodes;

        final List<ListenerRef> listenerRefs = new LinkedList<ListenerRef>();
        for (GradleProjectExtensionNodes singleExtensionNodes: extensionNodes) {
            listenerRefs.add(singleExtensionNodes.addNodeChangeListener(simpleChangeListener));
        }

        final Sources sources = ProjectUtils.getSources(project);
        sources.addChangeListener(changeListener);
        project.addModelChangeListener(changeListener);

        Runnable prevTask = cleanupTaskRef.getAndSet(new Runnable() {
            @Override
            public void run() {
                sources.removeChangeListener(changeListener);
                project.removeModelChangeListener(changeListener);

                for (ListenerRef ref: listenerRefs) {
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

    private void addDependencies(List<SingleNodeFactory> toPopulate) {
        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new GradleDependenciesNode(project);
            }
        });
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

    private Children createSubprojectsChild(List<NbGradleModule> children) {
        return Children.create(new SubProjectsChildFactory(project, children), true);
    }

    private static Action createOpenAction(String caption,
            Collection<NbGradleModule> projects) {
        return OpenProjectsAction.createFromModules(caption, projects);
    }

    private void addChildren(List<SingleNodeFactory> toPopulate) {
        NbGradleModule shownModule = getShownModule();
        final List<NbGradleModule> immediateChildren = shownModule.getChildren();
        if (immediateChildren.isEmpty()) {
            return;
        }
        final List<NbGradleModule> children = NbModelUtils.getAllChildren(shownModule);

        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new FilterNode(
                        createSimpleNode(),
                        createSubprojectsChild(immediateChildren),
                        Lookups.fixed(immediateChildren.toArray())) {
                    @Override
                    public String getName() {
                        return "SubProjectsNode_" + getShownModule().getUniqueName();
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

    private void addListedDirs(List<SingleNodeFactory> toPopulate) {
        for (File listedDir: project.getCurrentModel().getMainModule().getListedDirs()) {
            FileObject listedDirObj = FileUtil.toFileObject(listedDir);
            if (listedDirObj != null) {
                final DataFolder listedFolder = DataFolder.findFolder(listedDirObj);
                toPopulate.add(new SingleNodeFactory() {
                    @Override
                    public Node createNode() {
                        return listedFolder.getNodeDelegate().cloneNode();
                    }
                });
            }
        }
    }

    private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        List<GradleProjectExtensionNodes> extensionNodes = currentExtensions;
        if (extensionNodes != null) {
            for (GradleProjectExtensionNodes nodes: extensionNodes) {
                toPopulate.addAll(nodes.getNodeFactories());
            }
        }

        // TODO: Except for addProjectFiles, other nodes should be displayed by
        //   an extension (Java extension).
        addSources(toPopulate);
        addListedDirs(toPopulate);

        addChildren(toPopulate);

        addDependencies(toPopulate);
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
