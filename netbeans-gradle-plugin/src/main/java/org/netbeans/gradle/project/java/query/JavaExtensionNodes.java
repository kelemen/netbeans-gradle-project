package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;

public final class JavaExtensionNodes
implements
        GradleProjectExtensionNodes,
        JavaModelChangeListener {

    private static final Logger LOGGER = Logger.getLogger(JavaExtensionNodes.class.getName());

    private final JavaExtension javaExt;
    private final ChangeSupport nodeChanges;

    public JavaExtensionNodes(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.nodeChanges = new ChangeSupport(this);
    }

    @Override
    public void onModelChange() {
        // FIXME: We currently rely on the undocumented fact, that nodes are
        // always reloaded after a model reload.
        //
        // We will not call fireNodeChanges() until the implementation to
        // request the reloading of nodes is more clever and merges multiple
        // node reload requests into a single reload.
        //
        // Using UpdaterSwingTaskExecutor (always invoke later) should be
        // sufficient.

        // fireNodeChanges();
    }

    @Override
    public NbListenerRef addNodeChangeListener(final Runnable listener) {
        if (listener == null) throw new NullPointerException("listener");

        final ChangeListener listenerWrapper = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                listener.run();
            }
        };

        nodeChanges.addChangeListener(listenerWrapper);
        return new NbListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                nodeChanges.removeChangeListener(listenerWrapper);
                registered = false;
            }
        };
    }

    private void addListedDirs(List<SingleNodeFactory> toPopulate) {
        for (NbListedDir listedDir: javaExt.getCurrentModel().getMainModule().getListedDirs()) {
            FileObject listedDirObj = FileUtil.toFileObject(listedDir.getDirectory());
            if (listedDirObj != null) {
                final String dirName = listedDir.getName();
                final DataFolder listedFolder = DataFolder.findFolder(listedDirObj);

                toPopulate.add(new SingleNodeFactory() {
                    @Override
                    public Node createNode() {
                        return new FilterNode(listedFolder.getNodeDelegate().cloneNode()) {
                            @Override
                            public String getDisplayName() {
                                return dirName;
                            }
                        };
                    }
                });
            }
        }
    }

    private void addDependencies(List<SingleNodeFactory> toPopulate) {
        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new JavaDependenciesNode(javaExt);
            }
        });
    }

    private void addSourceRoots(List<SingleNodeFactory> toPopulate) {
        List<NamedSourceRoot> namedRoots = javaExt.getCurrentModel().getMainModule().getNamedSourceRoots();

        for (final NamedSourceRoot root: namedRoots) {
            final SourceGroup group = GradleProjectSources.tryCreateSourceGroup(root);
            if (group == null) {
                continue;
            }

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return PackageView.createPackageView(group);
                }
            });
        }
    }

    private void fireNodeChanges() {
        nodeChanges.fireChange();
    }

    private boolean createDir(File dir) {
        if (dir.isDirectory()) {
            return false;
        }
        boolean created = dir.mkdirs();
        if (!created) {
            LOGGER.log(Level.INFO, "Failed to create new directory: {0}", dir);
        }

        return created;
    }

    private boolean createDirs(Collection<File> dirs) {
        boolean created = false;

        for (File dir: dirs ){
            if (createDir(dir)) {
                created = true;
            }
        }

        return created;
    }

    public void createDirectories() {
        NbJavaModule module = javaExt.getCurrentModel().getMainModule();
        boolean changed = false;

        try {
            for (JavaSourceSet sourceSet: module.getSources()) {
                for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                    if (createDirs(sourceGroup.getSourceRoots())) {
                        changed = true;
                    }
                }
            }

            for (NbListedDir listedDir: module.getListedDirs()) {
                if (createDir(listedDir.getDirectory())) {
                    changed = true;
                }
            }
        } finally {
            if (changed) {
                fireNodeChanges();
            }
        }
    }

    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<SingleNodeFactory>();

        addSourceRoots(result);
        addListedDirs(result);
        addDependencies(result);

        return result;
    }
}
