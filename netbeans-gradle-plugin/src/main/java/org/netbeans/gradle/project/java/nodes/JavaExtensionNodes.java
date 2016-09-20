package org.netbeans.gradle.project.java.nodes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes;
import org.netbeans.gradle.project.api.nodes.ManualRefreshedNodes;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NamedSourceRoot;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.java.query.GradleProjectSources;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.view.NodeUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;

@ManualRefreshedNodes
public final class JavaExtensionNodes
implements
        GradleProjectExtensionNodes {

    private final JavaExtension javaExt;

    public JavaExtensionNodes(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.javaExt = javaExt;
    }

    private PropertySource<JavaSourcesDisplayMode> javaSourcesDisplayMode() {
        return javaExt.getProjectProperties().javaSourcesDisplayMode().getActiveSource();
    }

    @Override
    public NbListenerRef addNodeChangeListener(Runnable listener) {
        ListenerRef ref1 = javaExt.addModelChangeListener(listener);
        ListenerRef ref2 = javaExt.getSourceDirsHandler().addDirsCreatedListener(listener);
        ListenerRef ref3 = javaSourcesDisplayMode().addChangeListener(listener);
        return NbListenerRefs.asNbRef(ListenerRegistries.combineListenerRefs(ref1, ref2, ref3));
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

    private NbJavaModule getCurrentModel() {
        return javaExt.getCurrentModel().getMainModule();
    }

    private static JavaSourceSet[] sortSourceSets(List<JavaSourceSet> sourceSets) {
        JavaSourceSet[] result = sourceSets.toArray(new JavaSourceSet[sourceSets.size()]);
        Arrays.sort(result, new Comparator<JavaSourceSet>() {
            @Override
            public int compare(JavaSourceSet o1, JavaSourceSet o2) {
                return NamedSourceRoot.compareSourceSetNames(o1.getName(), o2.getName());
            }
        });
        return result;
    }

    private void addSourceRootsBySourceSet(List<SingleNodeFactory> toPopulate) {
        JavaSourceSet[] sourceSets = sortSourceSets(getCurrentModel().getSources());

        for (JavaSourceSet sourceSet: sourceSets) {
            toPopulate.add(JavaSourceSetNode.createFactory(javaExt, sourceSet.getName()));
        }
    }

    private void addSourceRootsStandard(List<SingleNodeFactory> toPopulate) {
        List<NamedSourceRoot> namedRoots = getCurrentModel().getNamedSourceRoots();

        for (final NamedSourceRoot root: namedRoots) {
            SingleNodeFactory nodeFactory = GradleProjectSources.tryCreateSourceGroupNodeFactory(root);
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }
    }

    private void addSourceRoots(List<SingleNodeFactory> toPopulate) {
        switch (javaSourcesDisplayMode().getValue()) {
            case DEFAULT_MODE:
                addSourceRootsStandard(toPopulate);
                break;
            case GROUP_BY_SOURCESET:
                addSourceRootsBySourceSet(toPopulate);
                break;
            default:
                throw new AssertionError(javaSourcesDisplayMode().getValue().name());
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
}
