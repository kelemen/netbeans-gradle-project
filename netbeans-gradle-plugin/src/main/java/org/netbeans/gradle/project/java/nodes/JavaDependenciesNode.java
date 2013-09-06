package org.netbeans.gradle.project.java.nodes;

import java.awt.Image;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbJavaDependency;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbModuleDependency;
import org.netbeans.gradle.project.java.model.NbUriDependency;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public final class JavaDependenciesNode extends AbstractNode implements JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(JavaDependenciesNode.class.getName());

    private final DependenciesChildFactory childFactory;

    public JavaDependenciesNode(JavaExtension javaExt) {
        this(new DependenciesChildFactory(javaExt));
    }

    private JavaDependenciesNode(DependenciesChildFactory childFactory) {
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
                final Collection<SingleNodeFactory> dependencies,
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

        private static List<SingleNodeFactory> filesToNodes(Collection<File> files) {
            List<SingleNodeFactory> result = new ArrayList<SingleNodeFactory>(files.size());
            for (File file: files) {
                result.add(new FileDependency(file));
            }
            return result;
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();

            for (JavaSourceSet sourceSet: mainModule.getSources()) {
                JavaClassPaths classpaths = sourceSet.getClasspaths();

                // TODO: 1. Adjust node names (localize)
                //       2. Display project output jars
                //       3. Remove inherited dependencies: Detect this by
                //          finding the output dir of a source set as a dependency.
                //       4. Order dependencies

                List<SingleNodeFactory> compileNodes = filesToNodes(classpaths.getCompileClasspaths());
                addDependencyGroup("Compile for "+ sourceSet.getName(), compileNodes, toPopulate);

                List<SingleNodeFactory> runtimeNodes = filesToNodes(classpaths.getRuntimeClasspaths());
                addDependencyGroup("Runtime for "+ sourceSet.getName(), runtimeNodes, toPopulate);
            }

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
        private final List<SingleNodeFactory> dependencies;

        public DependencyGroupChildFactory(Collection<SingleNodeFactory> dependencies) {
            this.dependencies = new ArrayList<SingleNodeFactory>(dependencies);
        }

        protected void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            toPopulate.addAll(dependencies);
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

    private static final class FileDependency implements SingleNodeFactory {
        private final File file;

        public FileDependency(File file) {
            assert file != null;

            this.file = file;
        }

        @Override
        public Node createNode() {
            File normalizedFile = FileUtil.normalizeFile(file);
            FileObject fileObj = normalizedFile != null ? FileUtil.toFileObject(normalizedFile) : null;
            if (fileObj == null) {
                LOGGER.log(Level.WARNING, "Dependency is not available: {0}", file);
                return null;
            }

            final DataObject dataObj;
            try {
                dataObj = DataObject.find(fileObj);
            } catch (DataObjectNotFoundException ex) {
                LOGGER.log(Level.INFO, "Unexpected DataObjectNotFoundException for file: " + file, ex);
                return null;
            }
            return dataObj.getNodeDelegate().cloneNode();
        }
    }
}
