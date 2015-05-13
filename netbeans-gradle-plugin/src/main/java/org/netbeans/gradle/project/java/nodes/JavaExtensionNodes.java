package org.netbeans.gradle.project.java.nodes;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.ManualRefreshedNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.java.query.GradleProjectSources;
import org.netbeans.gradle.project.view.NodeUtils;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;

@ManualRefreshedNodes
public final class JavaExtensionNodes
implements
        GradleProjectExtensionNodes,
        JavaModelChangeListener {

    private final JavaExtension javaExt;
    private final ChangeListenerManager nodeChangeListeners;

    public JavaExtensionNodes(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.javaExt = javaExt;
        this.nodeChangeListeners = new GenericChangeListenerManager();

        javaExt.getSourceDirsHandler().addDirsCreatedListener(new Runnable() {
            @Override
            public void run() {
                fireNodeChangeEvent();
            }
        });
    }

    private void fireNodeChangeEvent() {
        nodeChangeListeners.fireEventually();
    }

    @Override
    public void onModelChange() {
        fireNodeChangeEvent();
    }

    @Override
    public NbListenerRef addNodeChangeListener(Runnable listener) {
        return NbListenerRefs.asNbRef(nodeChangeListeners.registerListener(listener));
    }

    private void addListedDirs(List<SingleNodeFactory> toPopulate) {
        List<NbListedDir> allListedDirs = javaExt.getCurrentModel().getMainModule().getListedDirs();
        if (allListedDirs.isEmpty()) {
            return;
        }

        for (NbListedDir listedDir: allListedDirs) {
            FileObject listedDirObj = FileUtil.toFileObject(listedDir.getDirectory());
            if (listedDirObj != null) {
                String dirName = listedDir.getName();
                SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(listedDirObj, dirName);
                if (nodeFactory != null) {
                    toPopulate.add(nodeFactory);
                }
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
            SourceGroup group = GradleProjectSources.tryCreateSourceGroup(root);
            if (group == null) {
                continue;
            }
            toPopulate.add(new SourceRootNodeFactory(root, group));
        }
    }

    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<>();

        addSourceRoots(result);
        addListedDirs(result);
        addDependencies(result);

        return result;
    }

    private static class SourceRootNodeFactory implements SingleNodeFactory {
        private final Object sourceGroupKey;
        private final SourceGroup group;

        public SourceRootNodeFactory(Object sourceGroupKey, SourceGroup group) {
            this.sourceGroupKey = sourceGroupKey;
            this.group = group;
        }

        @Override
        public Node createNode() {
            return PackageView.createPackageView(group);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.sourceGroupKey);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)return false;
            if (getClass() != obj.getClass()) return false;

            final SourceRootNodeFactory other = (SourceRootNodeFactory)obj;
            return Objects.equals(this.sourceGroupKey, other.sourceGroupKey);
        }
    }
}
