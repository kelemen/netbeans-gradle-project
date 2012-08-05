package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleClassPathProvider implements ClassPathProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleClassPathProvider.class.getName());

    private static final String RELATIVE_OUTPUT_PATH = "build/classes/main/";
    private static final String RELATIVE_TEST_OUTPUT_PATH = "build/classes/test/";

    private final NbGradleProject project;
    private final ConcurrentMap<ClassPathType, List<PathResourceImplementation>> classpathResources;
    private final ConcurrentMap<ClassPathType, ClassPath> classpaths;

    private final PropertyChangeSupport changes;

    // EnumMap is not a ConcurrentMap, so it cannot be used.
    @SuppressWarnings("MapReplaceableByEnumMap")
    public GradleClassPathProvider(NbGradleProject project) {
        this.project = project;

        int classPathTypeCount = ClassPathType.values().length;
        this.classpathResources = new ConcurrentHashMap<ClassPathType, List<PathResourceImplementation>>(classPathTypeCount);
        this.classpaths = new ConcurrentHashMap<ClassPathType, ClassPath>(classPathTypeCount);
        this.changes = new PropertyChangeSupport(this);
    }

    // These PropertyChangeListener methods are declared because
    // for some reason, NetBeans want to use them through reflection.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private static FileObject getIdeaModuleDir(NbProjectModel projectModel, IdeaModule module) {
        File contentRoot = NbProjectModelUtils.getIdeaModuleDir(projectModel, module);
        return contentRoot != null ? FileUtil.toFileObject(contentRoot) : null;
    }

    private FileType getTypeOfFile(NbProjectModel projectModel, IdeaModule module, FileObject file) {
        Map<SourceFileType, List<File>> sourceRoots = GradleProjectSources.getSourceRoots(module);

        for (Map.Entry<SourceFileType, List<File>> entry: sourceRoots.entrySet()) {
            for (File sourceRoot: entry.getValue()) {
                FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
                if (sourceRootObj != null && FileUtil.getRelativePath(sourceRootObj, file) != null) {
                    return sourceTypeToFileType(entry.getKey());
                }
            }
        }

        FileObject contentRoot = getIdeaModuleDir(projectModel, module);
        if (contentRoot == null) {
            return null;
        }

        FileObject outputDir = contentRoot.getFileObject(RELATIVE_OUTPUT_PATH);
        if (outputDir != null && FileUtil.getRelativePath(outputDir, file) != null) {
            return FileType.COMPILED;
        }

        FileObject testOutputDir = contentRoot.getFileObject(RELATIVE_TEST_OUTPUT_PATH);
        if (testOutputDir != null && FileUtil.getRelativePath(testOutputDir, file) != null) {
            return FileType.COMPILED_TEST;
        }

        return null;
    }

    private static FileType sourceTypeToFileType(SourceFileType sourceType) {
        switch (sourceType) {
            case RESOURCE:
                return FileType.RESOURCE;
            case SOURCE:
                return FileType.SOURCE;
            case TEST_RESOURCE:
                return FileType.TEST_RESOURCE;
            case TEST_SOURCE:
                return FileType.TEST_SOURCE;
            default:
                throw new AssertionError("Unexpected source type: " + sourceType);
        }
    }

    private FileType getTypeOfFile(NbProjectModel projectModel, FileObject file) {
        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
        if (mainModule != null) {
            FileType result = getTypeOfFile(projectModel, mainModule, file);
            if (result != null) {
                return result;
            }
            for (IdeaModule dependency: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
                FileType depResult = getTypeOfFile(projectModel, dependency, file);
                if (depResult != null) {
                    return depResult;
                }
            }
        }

        return null;
    }

    private static ClassPathType getClassPathType(FileType fileType, String type) {
        if (fileType == null) {
            return null;
        }

        if (ClassPath.SOURCE.equals(type)) {
            return fileType.isTest()
                    ? ClassPathType.SOURCES_FOR_TEST
                    : ClassPathType.SOURCES;
        }
        else if (ClassPath.BOOT.equals(type)) {
            return fileType.isTest()
                    ? ClassPathType.BOOT_FOR_TEST
                    : ClassPathType.BOOT;
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return fileType.isTest()
                    ? ClassPathType.COMPILE_FOR_TEST
                    : ClassPathType.COMPILE;
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return fileType.isTest()
                    ? ClassPathType.RUNTIME_FOR_TEST
                    : ClassPathType.RUNTIME;
        }

        return null;
    }

    private ClassPathType getClassPathType(NbProjectModel projectModel, FileObject file, String type) {
        FileType fileType = getTypeOfFile(projectModel, file);
        return getClassPathType(fileType, type);
    }

    private List<PathResourceImplementation> getPathResources(List<File>... fileGroups) {
        int size = 0;
        for (List<?> fileGroup: fileGroups) {
            size += fileGroup.size();
        }

        List<PathResourceImplementation> result = new ArrayList<PathResourceImplementation>(size);
        for (List<File> fileGroup: fileGroups) {
            for (File file: fileGroup) {
                URL url = FileUtil.urlForArchiveOrDir(file);
                result.add(ClassPathSupport.createResource(url));
            }
        }
        return result;
    }

    private void setClassPathResources(
            ClassPathType classPathType,
            List<PathResourceImplementation> paths) {
        classpathResources.put(classPathType, Collections.unmodifiableList(paths));
    }

    private void loadPathResourcesForSources(NbProjectModel projectModel) {
        List<File> sources = new LinkedList<File>();
        List<File> testSources = new LinkedList<File>();

        Map<SourceFileType, List<File>> sourceRoots;

        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);

        sourceRoots = GradleProjectSources.getSourceRoots(mainModule);
        sources.addAll(sourceRoots.get(SourceFileType.SOURCE));
        testSources.addAll(sourceRoots.get(SourceFileType.TEST_SOURCE));

        for (IdeaModule module: NbProjectModelUtils.getIdeaProjectDependencies(mainModule)) {
            sourceRoots = GradleProjectSources.getSourceRoots(module);
            sources.addAll(sourceRoots.get(SourceFileType.SOURCE));
            testSources.addAll(sourceRoots.get(SourceFileType.TEST_SOURCE));
        }

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> sourcePaths = getPathResources(sources);
        setClassPathResources(ClassPathType.SOURCES, sourcePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testSourcePaths = getPathResources(sources, testSources);
        setClassPathResources(ClassPathType.SOURCES_FOR_TEST, testSourcePaths);
    }

    private static void addModuleClassPaths(
            NbProjectModel projectModel,
            IdeaModule project,
            List<File> paths,
            List<File> testPaths) {

        File projectDir = NbProjectModelUtils.getIdeaModuleDir(projectModel, project);

        if (paths != null) {
            paths.add(new File(projectDir, RELATIVE_OUTPUT_PATH));
        }

        if (testPaths != null) {
            testPaths.add(new File(projectDir, RELATIVE_TEST_OUTPUT_PATH));
        }
    }

    private static void addExternalClassPaths(
            ExternalDependency dependency,
            List<File> paths) {
        paths.add(dependency.getFile());
    }

    private void loadPathResources(NbProjectModel projectModel) {
        loadPathResourcesForSources(projectModel);

        List<File> compile = new LinkedList<File>();
        List<File> testCompile = new LinkedList<File>();
        List<File> runtime = new LinkedList<File>();
        List<File> testRuntime = new LinkedList<File>();

        IdeaModule mainModule = NbProjectModelUtils.getMainIdeaModule(projectModel);
        addModuleClassPaths(projectModel, mainModule, compile, testCompile);

        for (IdeaDependency dependency: NbProjectModelUtils.getIdeaDependencies(mainModule)) {
            String scope = dependency.getScope().getScope();
            if ("COMPILE".equals(scope)) {
                if (dependency instanceof ExternalDependency) {
                    addExternalClassPaths((ExternalDependency)dependency, compile);
                }
                else if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;
                    addModuleClassPaths(projectModel, moduleDep.getDependencyModule(),
                            compile, null);
                }
            }
            else if ("RUNTIME".equals(scope)) {
                if (dependency instanceof ExternalDependency) {
                    addExternalClassPaths((ExternalDependency)dependency, runtime);
                }
                else if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;
                    addModuleClassPaths(projectModel, moduleDep.getDependencyModule(),
                            runtime, null);
                }
            }
            else if ("TEST".equals(scope)) {
                // FIXME: This should only be applied to first level dependencies
                if (dependency instanceof ExternalDependency) {
                    addExternalClassPaths((ExternalDependency)dependency, testCompile);
                }
                else if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;
                    addModuleClassPaths(projectModel, moduleDep.getDependencyModule(),
                            testCompile, null);
                }
            }
            else {
                // FIXME: This should only be applied to first level dependencies
                if (dependency instanceof ExternalDependency) {
                    addExternalClassPaths((ExternalDependency)dependency, testRuntime);
                }
                else if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;
                    addModuleClassPaths(projectModel, moduleDep.getDependencyModule(),
                            testRuntime, null);
                }
            }
        }

        List<PathResourceImplementation> jdk = new LinkedList<PathResourceImplementation>();
        for (ClassPath.Entry entry: JavaPlatform.getDefault().getBootstrapLibraries().entries()) {
            jdk.add(ClassPathSupport.createResource(entry.getURL()));
        }

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> compilePaths = getPathResources(compile);
        setClassPathResources(ClassPathType.COMPILE, compilePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testCompilePaths = getPathResources(compile, testCompile);
        setClassPathResources(ClassPathType.COMPILE_FOR_TEST, testCompilePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> runtimePaths = getPathResources(compile, runtime);
        setClassPathResources(ClassPathType.RUNTIME, runtimePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testRuntimePaths = getPathResources(
                compile, testCompile, runtime, testRuntime);
        setClassPathResources(ClassPathType.RUNTIME_FOR_TEST, testRuntimePaths);

        List<PathResourceImplementation> bootPaths = new ArrayList<PathResourceImplementation>(
                runtimePaths.size() + jdk.size());
        bootPaths.addAll(runtimePaths);
        bootPaths.addAll(jdk);
        setClassPathResources(ClassPathType.BOOT, bootPaths);

        List<PathResourceImplementation> testBootPaths = new ArrayList<PathResourceImplementation>(
                runtimePaths.size() + jdk.size());
        testBootPaths.addAll(testRuntimePaths);
        testBootPaths.addAll(jdk);
        setClassPathResources(ClassPathType.BOOT_FOR_TEST, testBootPaths);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
            }
        });
    }

    private void loadClassPath(ClassPathType classPathType) {
        classpaths.putIfAbsent(
                classPathType,
                ClassPathFactory.createClassPath(new GradleClassPaths(classPathType)));
    }

    private void loadClassPaths() {
        loadClassPath(ClassPathType.COMPILE);
        loadClassPath(ClassPathType.COMPILE_FOR_TEST);
        loadClassPath(ClassPathType.RUNTIME);
        loadClassPath(ClassPathType.RUNTIME_FOR_TEST);
        loadClassPath(ClassPathType.BOOT);
        loadClassPath(ClassPathType.BOOT_FOR_TEST);
        loadClassPath(ClassPathType.SOURCES);
        loadClassPath(ClassPathType.SOURCES_FOR_TEST);
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        NbProjectModel projectModel = project.tryGetCachedProject();

        if (SwingUtilities.isEventDispatchThread()) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        "findClassPath has been called from the EDT: {0}",
                        Arrays.toString(Thread.currentThread().getStackTrace()));
            }

            if (projectModel == null) {
                DelayedClassPaths delayedClassPaths = new DelayedClassPaths(file, type);
                delayedClassPaths.startFetchingPaths();
                return ClassPathFactory.createClassPath(delayedClassPaths);
            }
        }

        if (projectModel == null) {
            projectModel = project.loadProject();
        }

        ClassPathType classPathType = getClassPathType(projectModel, file, type);
        if (classPathType == null) {
            return null;
        }

        ClassPath result = classpaths.get(classPathType);
        if (result != null) {
            return result;
        }

        loadPathResources(projectModel);
        loadClassPaths();

        return classpaths.get(classPathType);
    }

    private class GradleClassPaths implements ClassPathImplementation {
        private final ClassPathType classPathType;

        public GradleClassPaths(ClassPathType classPathType) {
            assert classPathType != null;
            this.classPathType = classPathType;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            return classpathResources.get(classPathType);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changes.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changes.removePropertyChangeListener(listener);
        }
    }

    private class DelayedClassPaths implements ClassPathImplementation {
        private final FileObject file;
        private final String type;
        private volatile ClassPathType classPathType;

        public DelayedClassPaths(FileObject file, String type) {
            this.file = file;
            this.type = type;
            this.classPathType = null;
        }

        public void startFetchingPaths() {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    NbProjectModel projectModel = project.loadProject();
                    classPathType = getClassPathType(projectModel, file, type);

                    loadPathResources(projectModel);
                    loadClassPaths();
                }
            });
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            ClassPathType currentType = classPathType;
            return currentType != null
                    ? classpathResources.get(currentType)
                    : Collections.<PathResourceImplementation>emptyList();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changes.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changes.removePropertyChangeListener(listener);
        }
    }

    private enum FileType {
        SOURCE(false),
        RESOURCE(false),
        TEST_SOURCE(true),
        TEST_RESOURCE(true),
        COMPILED(false),
        COMPILED_TEST(true);

        private final boolean test;

        private FileType(boolean test) {
            this.test = test;
        }

        public boolean isTest() {
            return test;
        }
    }

    private enum ClassPathType {
        SOURCES,
        SOURCES_FOR_TEST,
        RUNTIME,
        RUNTIME_FOR_TEST,
        BOOT,
        BOOT_FOR_TEST,
        COMPILE,
        COMPILE_FOR_TEST
    }
}
