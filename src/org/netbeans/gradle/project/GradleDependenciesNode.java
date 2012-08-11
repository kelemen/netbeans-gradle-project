package org.netbeans.gradle.project;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.gradle.project.model.NbDependency;
import org.netbeans.gradle.project.model.NbDependencyType;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbModuleDependency;
import org.netbeans.gradle.project.model.NbUriDependency;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

public final class GradleDependenciesNode extends AbstractNode {
    private static final Logger LOGGER = Logger.getLogger(GradleDependenciesNode.class.getName());

    public GradleDependenciesNode(NbGradleProject project) {
        super(createChildren(project));
    }

    private static Children createChildren(NbGradleProject project) {
        return Children.create(new DependenciesChildFactory(project), true);
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
        return NbBundle.getMessage(GradleDependenciesNode.class, "LBL_Dependencies");
    }

    private static class DependenciesChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            ChangeListener {
        private final NbGradleProject project;

        public DependenciesChildFactory(NbGradleProject project) {
            this.project = project;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refresh(false);
        }

        @Override
        protected void addNotify() {
            project.addModelChangeListener(this);
        }

        @Override
        protected void removeNotify() {
            project.removeModelChangeListener(this);
        }

        private void addDependencyGroup(
                final String groupName,
                final Collection<NbDependency> dependencies,
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

        private static List<NbDependency> orderDependencies(Collection<NbDependency> dependencies) {
            List<NbDependency> result = new ArrayList<NbDependency>(dependencies);
            final Collator textComparer = Collator.getInstance(Locale.US);
            final Map<Class<?>, Integer> classOrder = new HashMap<Class<?>, Integer>();
            classOrder.put(NbModuleDependency.class, 0);
            classOrder.put(NbUriDependency.class, 1);

            Collections.sort(result, new Comparator<NbDependency>() {
                @Override
                public int compare(NbDependency o1, NbDependency o2) {
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
                            return Integer.compare(index1, index2);
                        }
                    }

                    return textComparer.compare(o1.getShortName(), o2.getShortName());
                }
            });
            return result;
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            NbGradleModule mainModule = project.getCurrentModel().getMainModule();

            Set<NbDependency> compile = new LinkedHashSet<NbDependency>(
                    NbModelUtils.getAllDependencies(mainModule, NbDependencyType.COMPILE));
            Set<NbDependency> runtime = new LinkedHashSet<NbDependency>(
                    NbModelUtils.getAllDependencies(mainModule, NbDependencyType.RUNTIME));
            Set<NbDependency> testCompile = new LinkedHashSet<NbDependency>(
                    NbModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_COMPILE));
            Set<NbDependency> testRuntime = new LinkedHashSet<NbDependency>(
                    NbModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_RUNTIME));

            testRuntime.removeAll(runtime);
            testRuntime.removeAll(testCompile);
            runtime.removeAll(compile);
            testCompile.removeAll(compile);

            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_CompileDependencies"),
                    orderDependencies(compile), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_RuntimeDependencies"),
                    orderDependencies(runtime), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_TestCompileDependencies"),
                    orderDependencies(testCompile), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_TestRuntimeDependencies"),
                    orderDependencies(testRuntime), toPopulate);

            LOGGER.fine("Dependencies for the Gradle project were found.");
        }

        @Override
        protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
            ProgressHandle progress = ProgressHandleFactory.createHandle(
                    NbBundle.getMessage(GradleProjectChildFactory.class, "LBL_LookUpExternalDependencies"));
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

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }

    private static class DependencyGroupChildFactory extends ChildFactory<SingleNodeFactory> {
        private final List<NbDependency> dependencies;

        public DependencyGroupChildFactory(Collection<NbDependency> dependencies) {
            this.dependencies = new ArrayList<NbDependency>(dependencies);
        }

        private void addModuleDependency(
                final NbModuleDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            FileObject moduleRoot = FileUtil.toFileObject(dependency.getModule().getModuleDir());

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
                                return dependency.getModule().getName();
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
                                return dependency.getModule().getName();
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
                NbDependency dependency,
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
            for (NbDependency dependency: dependencies) {
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
