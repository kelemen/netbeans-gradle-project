package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
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
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleProjectChildFactory
extends
        ChildFactory.Detachable<SingleNodeFactory> {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectChildFactory.class.getName());

    private final NbGradleProject project;
    private final AtomicReference<Runnable> cleanupTaskRef;

    public GradleProjectChildFactory(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.project = project;
        this.cleanupTaskRef = new AtomicReference<Runnable>(null);
    }

    private NbGradleModule getShownModule() {
        return project.getCurrentModel().getMainModule();
    }

    @Override
    protected void addNotify() {
        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refresh(false);
            }
        };

        final Sources sources = ProjectUtils.getSources(project);
        sources.addChangeListener(changeListener);
        project.addModelChangeListener(changeListener);

        Runnable prevTask = cleanupTaskRef.getAndSet(new Runnable() {
            @Override
            public void run() {
                sources.removeChangeListener(changeListener);
                project.removeModelChangeListener(changeListener);
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

    private void addGradleFile(
            FileObject file,
            List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        final DataObject fileData = DataObject.find(file);

        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new FilterNode(fileData.getNodeDelegate()) {
                    @Override
                    public boolean canRename() {
                        return false;
                    }

                    @Override
                    public Image getIcon(int type) {
                        return NbIcons.getGradleIcon();
                    }

                    @Override
                    public Image getOpenedIcon(int type) {
                        return getIcon(type);
                    }
                };
            }
        });
    }

    private void addProjectFiles(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        NbGradleModel model = project.getCurrentModel();
        FileObject buildGradle = model.getBuildFile();
        if (buildGradle != null) {
            addGradleFile(buildGradle, toPopulate);
        }

        FileObject settingsGradle = model.getSettingsFile();
        if (settingsGradle != null) {
            addGradleFile(settingsGradle, toPopulate);
        }
    }

    private Node createSimpleNode() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        return projectFolder.getNodeDelegate().cloneNode();
    }

    private Children createSubprojectsChild(List<NbGradleModule> children) {
        return Children.create(new SubProjectsChildFactory(project, children), true);
    }

    private void addChildren(List<SingleNodeFactory> toPopulate) {
        final List<NbGradleModule> children = getShownModule().getChildren();
        if (children.isEmpty()) {
            return;
        }

        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new FilterNode(
                        createSimpleNode(),
                        createSubprojectsChild(children),
                        Lookups.fixed(children.toArray())) {
                    @Override
                    public String getName() {
                        return "SubProjectsNode_" + getShownModule().getUniqueName();
                    }

                    @Override
                    public Action[] getActions(boolean context) {
                        return new Action[] {
                            new OpenProjectsAction(NbStrings.getOpenSubProjectsCaption(), children)
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
