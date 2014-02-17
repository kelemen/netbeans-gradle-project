package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.ManualRefreshedNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.JavaSourceGroupID;
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

@ManualRefreshedNodes
public final class JavaExtensionNodes
implements
        GradleProjectExtensionNodes,
        JavaModelChangeListener {

    private final JavaExtension javaExt;
    private final ChangeSupport nodeChanges;
    private final AtomicReference<NodesDescription> lastDisplayed;

    public JavaExtensionNodes(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.nodeChanges = new ChangeSupport(this);
        this.lastDisplayed = new AtomicReference<NodesDescription>(null);

        javaExt.getSourceDirsHandler().addDirsCreatedListener(new Runnable() {
            @Override
            public void run() {
                nodeChanges.fireChange();
            }
        });
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
            nodeChanges.fireChange();
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
        Set<SourceRootID> result = new HashSet<SourceRootID>();
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
                    ids.add(new SourceRootID(sourceSetName, groupName, root));
                }
            }
        }
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

    private Set<NbListedDir> addListedDirs(List<SingleNodeFactory> toPopulate) {
        List<NbListedDir> allListedDirs = javaExt.getCurrentModel().getMainModule().getListedDirs();
        if (allListedDirs.isEmpty()) {
            return Collections.emptySet();
        }

        Set<NbListedDir> result = CollectionUtils.newHashSet(allListedDirs.size());

        for (NbListedDir listedDir: allListedDirs) {
            FileObject listedDirObj = FileUtil.toFileObject(listedDir.getDirectory());
            if (listedDirObj != null) {
                final String dirName = listedDir.getName();
                final DataFolder listedFolder = DataFolder.findFolder(listedDirObj);

                result.add(listedDir);

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
            result.add(new SourceRootID(groupID.getSourceSetName(), groupID.getGroupName(), root.getRoot()));

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return PackageView.createPackageView(group);
                }
            });
        }

        return result;
    }

    @Override
    public List<SingleNodeFactory> getNodeFactories() {
        List<SingleNodeFactory> result = new LinkedList<SingleNodeFactory>();

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

        public SourceRootID(String sourceSetName, JavaSourceGroupName groupName, File sourceRoot) {
            assert sourceSetName != null;
            assert groupName != null;
            assert sourceRoot != null;

            this.sourceSetName = sourceSetName;
            this.groupName = groupName;
            this.sourceRoot = sourceRoot;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + sourceSetName.hashCode();
            hash = 67 * hash + groupName.hashCode();
            hash = 67 * hash + sourceRoot.hashCode();
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

            return sourceRoot == other.sourceRoot || sourceRoot.equals(other.sourceRoot);
        }
    }
}
