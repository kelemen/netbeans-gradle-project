package org.netbeans.gradle.project.java.nodes;

import java.awt.Image;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbDependencyType;
import org.netbeans.gradle.project.java.model.NbJavaDependency;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.java.model.NbModuleDependency;
import org.netbeans.gradle.project.java.model.NbUriDependency;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

public final class GradleDependenciesNode extends AbstractNode implements JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleDependenciesNode.class.getName());

    private final DependenciesChildFactory childFactory;

    public GradleDependenciesNode(JavaExtension javaExt) {
        this(new DependenciesChildFactory(javaExt));
    }

    private GradleDependenciesNode(DependenciesChildFactory childFactory) {
        super(createChildren(childFactory));

        this.childFactory = childFactory;
    }

    private static Children createChildren(DependenciesChildFactory childFactory) {
        return Children.create(childFactory, true);
    }

    @Override
    public void onModelChange() {
        childFactory.stateChanged();
    }

    @Override
    public Image getIcon(int type) {
        return NbIcons.getLibrariesIcon();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public String getDisplayName() {
        return NbStrings.getDependenciesNodeCaption();
    }

    private static class DependenciesChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory> {

        private final JavaExtension javaExt;

        public DependenciesChildFactory(JavaExtension javaExt) {
            if (javaExt == null) throw new NullPointerException("javaExt");
            this.javaExt = javaExt;
        }

        public void stateChanged() {
            refresh(false);
        }

        private void addDependencyGroup(
                final String groupName,
                final Collection<NbJavaDependency> dependencies,
                List<SingleNodeFactory> toPopulate) {

            if (dependencies.isEmpty()) {
                return;
            }

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new AbstractNode(Children.create(new DependencyGroupChildFactory(dependencies), true)) {
                        @Override
                        public Image getIcon(int type) {
                            return NbIcons.getLibrariesIcon();
                        }

                        @Override
                        public Image getOpenedIcon(int type) {
                            return getIcon(type);
                        }

                        @Override
                        public String getDisplayName() {
                            return groupName;
                        }
                    };
                }
            });
        }

        private static List<NbJavaDependency> orderDependencies(Collection<NbJavaDependency> dependencies) {
            List<NbJavaDependency> result = new ArrayList<NbJavaDependency>(dependencies);
            final Collator textComparer = Collator.getInstance(Locale.US);
            final Map<Class<?>, Integer> classOrder = new HashMap<Class<?>, Integer>();
            classOrder.put(NbModuleDependency.class, 0);
            classOrder.put(NbUriDependency.class, 1);

            Collections.sort(result, new Comparator<NbJavaDependency>() {
                @Override
                public int compare(NbJavaDependency o1, NbJavaDependency o2) {
                    if (o1.getClass() != o2.getClass()) {
                        Integer index1 = classOrder.get(o1.getClass());
                        Integer index2 = classOrder.get(o2.getClass());

                        if (index1 == null) {
                            if (index2 != null) {
                                return 1;
                            }
                        }
                        else if (index2 == null) {
                            // index1 != null
                            return -1;
                        }
                        else {
                            int index1Value = index1;
                            int index2Value = index2;
                            return index1Value == index2Value
                                    ? 0
                                    : (index1Value < index2Value ? -1 : 1);
                        }
                    }

                    return textComparer.compare(o1.getShortName(), o2.getShortName());
                }
            });
            return result;
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();

            Set<NbJavaDependency> compile = new LinkedHashSet<NbJavaDependency>(
                    NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.COMPILE));
            Set<NbJavaDependency> runtime = new LinkedHashSet<NbJavaDependency>(
                    NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.RUNTIME));
            Set<NbJavaDependency> testCompile = new LinkedHashSet<NbJavaDependency>(
                    NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_COMPILE));
            Set<NbJavaDependency> testRuntime = new LinkedHashSet<NbJavaDependency>(
                    NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_RUNTIME));

            testRuntime.removeAll(runtime);
            testRuntime.removeAll(testCompile);
            runtime.removeAll(compile);
            testCompile.removeAll(compile);

            addDependencyGroup(NbStrings.getCompileDependenciesNodeCaption(),
                    orderDependencies(compile), toPopulate);
            addDependencyGroup(NbStrings.getRuntimeDependenciesNodeCaption(),
                    orderDependencies(runtime), toPopulate);
            addDependencyGroup(NbStrings.getTestCompileDependenciesNodeCaption(),
                    orderDependencies(testCompile), toPopulate);
            addDependencyGroup(NbStrings.getTestRuntimeDependenciesNodeCaption(),
                    orderDependencies(testRuntime), toPopulate);

            LOGGER.fine("Dependencies for the Gradle project were found.");
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

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }

    private static class DependencyGroupChildFactory extends ChildFactory<SingleNodeFactory> {
        private final List<NbJavaDependency> dependencies;

        public DependencyGroupChildFactory(Collection<NbJavaDependency> dependencies) {
            this.dependencies = new ArrayList<NbJavaDependency>(dependencies);
        }

        private void addModuleDependency(
                final NbModuleDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            FileObject moduleRoot = FileUtil.toFileObject(dependency.getModule().getModuleDir());
            final String displayName = dependency.getModule().getDisplayName();

            if (moduleRoot != null) {
                final DataObject fileObject = DataObject.find(moduleRoot);
                toPopulate.add(new SingleNodeFactory() {
                    @Override
                    public Node createNode() {
                        return new FilterNode(fileObject.getNodeDelegate()) {
                            @Override
                            public Image getIcon(int type) {
                                return NbIcons.getGradleIcon();
                            }

                            @Override
                            public Image getOpenedIcon(int type) {
                                return getIcon(type);
                            }

                            @Override
                            public String getDisplayName() {
                                return displayName;
                            }
                        };
                    }
                });
            }
            else {
                toPopulate.add(new SingleNodeFactory() {
                    @Override
                    public Node createNode() {
                        return new FilterNode(Node.EMPTY) {
                            @Override
                            public Image getIcon(int type) {
                                return NbIcons.getGradleIcon();
                            }

                            @Override
                            public Image getOpenedIcon(int type) {
                                return getIcon(type);
                            }

                            @Override
                            public String getDisplayName() {
                                return displayName;
                            }
                        };
                    }
                });
            }
        }

        private void addFileDependency(
                NbUriDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            FileObject file = dependency.tryGetAsFileObject();
            if (file == null) {
                LOGGER.log(Level.WARNING, "Dependency is not available: {0}", dependency.getUri());
                return;
            }
            final DataObject fileObject = DataObject.find(file);
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return fileObject.getNodeDelegate().cloneNode();
                }
            });
        }

        private void addGenericDependency(
                NbJavaDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            final String nodeCaption = dependency.toString();
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new FilterNode(Node.EMPTY) {
                        @Override
                        public Image getIcon(int type) {
                            return NbIcons.getLibraryIcon();
                        }

                        @Override
                        public Image getOpenedIcon(int type) {
                            return getIcon(type);
                        }

                        @Override
                        public String getDisplayName() {
                            return nodeCaption;
                        }
                    };
                }
            });
        }

        protected void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            for (NbJavaDependency dependency: dependencies) {
                if (dependency instanceof NbModuleDependency) {
                    addModuleDependency((NbModuleDependency)dependency, toPopulate);
                }
                else if (dependency instanceof NbUriDependency) {
                    addFileDependency((NbUriDependency)dependency, toPopulate);
                }
                else {
                    addGenericDependency(dependency, toPopulate);
                }
            }
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

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }
}
