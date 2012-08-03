package org.netbeans.gradle.project;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
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

    private static class DependenciesChildFactory extends ChildFactory.Detachable<SingleNodeFactory> {
        private final NbGradleProject project;

        public DependenciesChildFactory(NbGradleProject project) {
            this.project = project;
        }

        @Override
        protected void addNotify() {
        }

        @Override
        protected void removeNotify() {
        }

        private void addDependencyGroup(
                final String groupName,
                final Collection<IdeaDependency> dependencies,
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

        private static void addUniqueDependency(IdeaDependency dependency, Map<Object, IdeaDependency> toAdd) {
            Object key = dependency;
            if (dependency instanceof ExternalDependency) {
                key = new ExternalDependencyKey((ExternalDependency)dependency);
            }
            if (dependency instanceof IdeaModuleDependency) {
                key = new ModuleDependencyKey((IdeaModuleDependency)dependency);
            }
            if (!toAdd.containsKey(key)) {
                toAdd.put(key, dependency);
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            NbProjectModel projectModel = project.loadProject();
            IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
            if (mainModule == null) {
                return;
            }

            final Map<Object, IdeaDependency> compile = new LinkedHashMap<Object, IdeaDependency>();
            final Map<Object, IdeaDependency> runtime = new LinkedHashMap<Object, IdeaDependency>();
            final Map<Object, IdeaDependency> testCompile = new LinkedHashMap<Object, IdeaDependency>();
            final Map<Object, IdeaDependency> testRuntime = new LinkedHashMap<Object, IdeaDependency>();

            for (IdeaDependency dependency: NbProjectModelUtils.getIdeaDependencies(mainModule)) {
                String scope = dependency.getScope().getScope();
                Map<Object, IdeaDependency> choosenGroup;
                if ("COMPILE".equals(scope)) {
                    choosenGroup = compile;
                }
                else if ("RUNTIME".equals(scope)) {
                    choosenGroup = runtime;
                }
                else if ("TEST".equals(scope)) {
                    choosenGroup = testCompile;
                }
                else {
                    choosenGroup = testRuntime;
                }
                addUniqueDependency(dependency, choosenGroup);
            }

            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_CompileDependencies"),
                    compile.values(), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_RuntimeDependencies"),
                    runtime.values(), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_TestCompileDependencies"),
                    testCompile.values(), toPopulate);
            addDependencyGroup(NbBundle.getMessage(GradleDependenciesNode.class, "LBL_TestRuntimeDependencies"),
                    testRuntime.values(), toPopulate);

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
        private final List<IdeaDependency> dependencies;

        public DependencyGroupChildFactory(Collection<IdeaDependency> dependencies) {
            this.dependencies = new ArrayList<IdeaDependency>(dependencies);
        }

        private void addModuleDependency(
                IdeaModuleDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            final String moduleName = dependency.getDependencyModule().getGradleProject().getPath();

            DomainObjectSet<? extends IdeaContentRoot> contentRoots
                    = dependency.getDependencyModule().getContentRoots();

            if (!contentRoots.isEmpty()) {
                File dependecyRoot = contentRoots.getAt(0).getRootDirectory();
                final DataObject fileObject = DataObject.find(FileUtil.toFileObject(dependecyRoot));
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
                                return moduleName;
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
                                return moduleName;
                            }
                        };
                    }
                });
            }
        }

        private void addFileDependency(
                ExternalDependency dependency,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {

            final DataObject fileObject = DataObject.find(FileUtil.toFileObject(dependency.getFile()));
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return fileObject.getNodeDelegate();
                }
            });
        }

        private void addGenericDependency(
                IdeaDependency dependency,
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
            for (IdeaDependency dependency: dependencies) {
                if (dependency instanceof IdeaModuleDependency) {
                    addModuleDependency((IdeaModuleDependency)dependency, toPopulate);
                }
                else if (dependency instanceof ExternalDependency) {
                    addFileDependency((ExternalDependency)dependency, toPopulate);
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

    private static class ModuleDependencyKey {
        private final String path;

        public ModuleDependencyKey(IdeaModuleDependency dependency) {
            this.path = dependency.getDependencyModule().getGradleProject().getPath();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 17 * hash + (this.path != null ? this.path.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ModuleDependencyKey other = (ModuleDependencyKey)obj;
            if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
                return false;
            }
            return true;
        }
    }

    private static class ExternalDependencyKey {
        private final File path;

        public ExternalDependencyKey(ExternalDependency dependency) {
            this.path = dependency.getFile();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + (this.path != null ? this.path.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ExternalDependencyKey other = (ExternalDependencyKey)obj;
            if (this.path != other.path && (this.path == null || !this.path.equals(other.path))) {
                return false;
            }
            return true;
        }
    }
}
