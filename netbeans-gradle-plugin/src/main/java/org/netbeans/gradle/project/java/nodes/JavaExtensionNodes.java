package org.netbeans.gradle.project.java.nodes;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.ManualRefreshedNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.JavaSourceGroupID;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.java.query.GradleProjectSources;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
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
    private final AtomicReference<NodesDescription> lastDisplayed;

    public JavaExtensionNodes(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.javaExt = javaExt;
        this.nodeChangeListeners = new GenericChangeListenerManager();
        this.lastDisplayed = new AtomicReference<>(null);

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
        NbJavaModule newModule = javaExt.getCurrentModel().getMainModule();

        boolean hasChanged;
        NodesDescription currentNodes;
        do {
            currentNodes = lastDisplayed.get();

            hasChanged = currentNodes == null
                    || !currentNodes.listedDirs.equals(getAvailableListedDirs(newModule))
                    || !currentNodes.sourceRoots.equals(getAvailableSourceRootIDs(newModule));
        } while (currentNodes != lastDisplayed.get());

        if (hasChanged) {
            fireNodeChangeEvent();
        }
    }

    private static Set<NbListedDir> getAvailableListedDirs(NbJavaModule newModule) {
        Set<NbListedDir> result = CollectionUtils.newHashSet(newModule.getListedDirs().size());
        for (NbListedDir listedDir: newModule.getListedDirs()) {
            result.add(listedDir);
        }
        return result;
    }

    private static Set<SourceRootID> getAvailableSourceRootIDs(NbJavaModule newModule) {
        return getAvailableSourceRootIDs(newModule.getSources());
    }

    private static Set<SourceRootID> getAvailableSourceRootIDs(List<JavaSourceSet> sourceSets) {
        Set<SourceRootID> result = new HashSet<>();
        for (JavaSourceSet sourceSet: sourceSets) {
            getAvailableSourceRootIDs(sourceSet, result);
        }
        return result;
    }

    private static void getAvailableSourceRootIDs(JavaSourceSet sourceSet, Set<SourceRootID> ids) {
        String sourceSetName = sourceSet.getName();
        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            JavaSourceGroupName groupName = group.getGroupName();
            for (File root: group.getSourceRoots()) {
                if (root.exists()) {
                    ids.add(new SourceRootID(sourceSetName, groupName, root, ExcludeIncludeRules.create(group)));
                }
            }
        }
    }

    @Override
    public NbListenerRef addNodeChangeListener(Runnable listener) {
        return NbListenerRefs.asNbRef(nodeChangeListeners.registerListener(listener));
    }

    private Set<NbListedDir> addListedDirs(List<SingleNodeFactory> toPopulate) {
        List<NbListedDir> allListedDirs = javaExt.getCurrentModel().getMainModule().getListedDirs();
        if (allListedDirs.isEmpty()) {
            return Collections.emptySet();
        }

        Set<NbListedDir> result = CollectionUtils.newHashSet(allListedDirs.size());

        for (NbListedDir listedDir: allListedDirs) {
            FileObject listedDirObj = FileUtil.toFileObject(listedDir.getDirectory());
            if (listedDirObj != null) {
                String dirName = listedDir.getName();
                SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(listedDirObj, dirName);
                if (nodeFactory != null) {
                    result.add(listedDir);
                    toPopulate.add(nodeFactory);
                }
            }
        }

        return result;
    }

    private void addDependencies(List<SingleNodeFactory> toPopulate) {
        toPopulate.add(new SingleNodeFactory() {
            @Override
            public Node createNode() {
                return new JavaDependenciesNode(javaExt);
            }
        });
    }

    private Set<SourceRootID> addSourceRoots(List<SingleNodeFactory> toPopulate) {
        List<NamedSourceRoot> namedRoots = javaExt.getCurrentModel().getMainModule().getNamedSourceRoots();
        Set<SourceRootID> result = CollectionUtils.newHashSet(namedRoots.size());

        for (final NamedSourceRoot root: namedRoots) {
            final SourceGroup group = GradleProjectSources.tryCreateSourceGroup(root);
            if (group == null) {
                continue;
            }

            JavaSourceGroupID groupID = root.getGroupID();
            result.add(new SourceRootID(
                    groupID.getSourceSetName(),
                    groupID.getGroupName(),
                    root.getRoot(),
                    root.getIncludeRules()));

            toPopulate.add(new SourceRootNodeFactory(root, group));
        }

        return result;
    }

    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<>();

        Set<SourceRootID> sourceRoots = addSourceRoots(result);
        Set<NbListedDir> listedDirs = addListedDirs(result);
        addDependencies(result);

        lastDisplayed.set(new NodesDescription(listedDirs, sourceRoots));

        return result;
    }

    private static final class NodesDescription {
        public final Set<NbListedDir> listedDirs;
        public final Set<SourceRootID> sourceRoots;

        public NodesDescription(Set<NbListedDir> listedDirs, Set<SourceRootID> sourceRoots) {
            this.listedDirs = listedDirs;
            this.sourceRoots = sourceRoots;
        }
    }

    private static final class SourceRootID {
        private final String sourceSetName;
        private final JavaSourceGroupName groupName;
        private final File sourceRoot;
        private final ExcludeIncludeRules includeRules;

        public SourceRootID(
                String sourceSetName,
                JavaSourceGroupName groupName,
                File sourceRoot,
                ExcludeIncludeRules includeRules) {

            assert sourceSetName != null;
            assert groupName != null;
            assert sourceRoot != null;
            assert includeRules != null;

            this.sourceSetName = sourceSetName;
            this.groupName = groupName;
            this.sourceRoot = sourceRoot;
            this.includeRules = includeRules;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + sourceSetName.hashCode();
            hash = 67 * hash + groupName.hashCode();
            hash = 67 * hash + sourceRoot.hashCode();
            hash = 67 * hash + includeRules.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (getClass() != obj.getClass()) return false;

            final SourceRootID other = (SourceRootID)obj;

            if (!sourceSetName.equals(other.sourceSetName)) {
                return false;
            }
            if (groupName != other.groupName) {
                return false;
            }
            if (!includeRules.equals(other.includeRules)) {
                return false;
            }

            return sourceRoot == other.sourceRoot || sourceRoot.equals(other.sourceRoot);
        }
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
