package org.netbeans.gradle.project.java.query;

import java.io.File;
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

    private final JavaExtension javaExt;
    private final ChangeSupport nodeChanges;
    private final AtomicReference<NbJavaModule> lastModule;

    public JavaExtensionNodes(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.nodeChanges = new ChangeSupport(this);
        this.lastModule = new AtomicReference<NbJavaModule>(null);

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
        NbJavaModule prevModule = lastModule.getAndSet(newModule);

        if (hasRelevantDifferences(newModule, prevModule)) {
            nodeChanges.fireChange();
        }
    }

    private static boolean hasRelevantDifferences(NbJavaModule module1, NbJavaModule module2) {
        if (module1 == module2) {
            // In practice this happens only when they are nulls.
            return false;
        }
        if (module1 == null || module2 == null) {
            return true;
        }

        if (!module1.getListedDirs().equals(module2.getListedDirs())) {
            return false;
        }

        Set<SourceRootID> ids1 = getSourceRootIDs(module1.getSources());
        Set<SourceRootID> ids2 = getSourceRootIDs(module2.getSources());

        return !ids1.equals(ids2);
    }

    private static Set<SourceRootID> getSourceRootIDs(List<JavaSourceSet> sourceSets) {
        Set<SourceRootID> result = new HashSet<SourceRootID>();
        for (JavaSourceSet sourceSet: sourceSets) {
            getSourceRootIDs(sourceSet, result);
        }
        return result;
    }

    private static void getSourceRootIDs(JavaSourceSet sourceSet, Set<SourceRootID> ids) {
        String sourceSetName = sourceSet.getName();
        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            JavaSourceGroupName groupName = group.getGroupName();
            for (File root: group.getSourceRoots()) {
                ids.add(new SourceRootID(sourceSetName, groupName, root));
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
