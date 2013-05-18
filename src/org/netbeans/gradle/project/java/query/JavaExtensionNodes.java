package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
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
        for (File listedDir: javaExt.getCurrentModel().getMainModule().getListedDirs()) {
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


    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<SingleNodeFactory>();

        addListedDirs(result);

        return result;
    }
}
