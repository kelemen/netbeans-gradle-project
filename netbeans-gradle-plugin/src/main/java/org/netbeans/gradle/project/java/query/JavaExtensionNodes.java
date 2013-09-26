package org.netbeans.gradle.project.java.query;

import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.project.GradleProjectSources;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

public final class JavaExtensionNodes implements GradleProjectExtensionNodes {
    private final JavaExtension javaExt;

    public JavaExtensionNodes(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    @Override
    public NbListenerRef addNodeChangeListener(Runnable listener) {
        if (listener == null) throw new NullPointerException("listener");
        // FIXME: We currently rely on the undocumented fact, that nodes are
        // always reloaded after a model reload.
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

    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<SingleNodeFactory>();

        addSourceRoots(result);
        addListedDirs(result);
        addDependencies(result);

        return result;
    }
}
