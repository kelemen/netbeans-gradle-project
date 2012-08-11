package org.netbeans.gradle.project;

import java.awt.Image;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

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
        addGradleFile(buildGradle, toPopulate);

        FileObject settingsGradle = model.getSettingsFile();
        if (settingsGradle != null) {
            addGradleFile(settingsGradle, toPopulate);
        }
    }


    private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
        Sources sources = ProjectUtils.getSources(project);

        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.SOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.RESOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.TEST_SOURCES), toPopulate);
        addSourceGroups(sources.getSourceGroups(GradleProjectConstants.TEST_RESOURCES), toPopulate);

        addDependencies(toPopulate);
        addProjectFiles(toPopulate);
    }

    @Override
    protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbBundle.getMessage(GradleProjectChildFactory.class, "LBL_LoadingProjectLayout"));
        progress.start();
        try {
            readKeys(toPopulate);
        } catch (DataObjectNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            progress.finish();
        }
        return true;
    }
}
