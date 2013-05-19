package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfoManager;
import org.netbeans.gradle.project.ProjectInfoRef;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.NbDependencyType;
import org.netbeans.gradle.project.java.model.NbJavaDependency;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbModuleDependency;
import org.netbeans.gradle.project.java.model.NbOutput;
import org.netbeans.gradle.project.java.model.NbSourceGroup;
import org.netbeans.gradle.project.java.model.NbSourceType;
import org.netbeans.gradle.project.java.model.NbUriDependency;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleClassPathProvider
implements
        ClassPathProvider,
        ProjectInitListener,
        JavaModelChangeListener {
    private static final Logger LOGGER = Logger.getLogger(GradleClassPathProvider.class.getName());

    private final JavaExtension javaExt;
    private final ConcurrentMap<ClassPathType, List<PathResourceImplementation>> classpathResources;
    private final ConcurrentMap<ClassPathType, ClassPath> classpaths;

    private final PropertyChangeSupport changes;
    private volatile ProjectPlatform currentPlatform;

    private final AtomicReference<ProjectInfoRef> infoRefRef;
    // This property is used to prevent eagrly loading a project due
    // to changes in the project settings. That is, if this class path provider
    // has never been used, there is no reason to calculate the classpath
    // due to a change in the settings.
    private final AtomicBoolean hasBeenUsed;

    // EnumMap is not a ConcurrentMap, so it cannot be used.
    @SuppressWarnings("MapReplaceableByEnumMap")
    public GradleClassPathProvider(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");

        this.javaExt = javaExt;
        this.currentPlatform = null;
        this.infoRefRef = new AtomicReference<ProjectInfoRef>(null);

        int classPathTypeCount = ClassPathType.values().length;
        this.classpathResources = new ConcurrentHashMap<ClassPathType, List<PathResourceImplementation>>(classPathTypeCount);
        this.classpaths = new ConcurrentHashMap<ClassPathType, ClassPath>(classPathTypeCount);
        this.hasBeenUsed = new AtomicBoolean(false);

        EventSource eventSource = new EventSource();
        this.changes = new PropertyChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    private ProjectInfoRef getInfoRef() {
        ProjectInfoRef result = infoRefRef.get();
        if (result == null) {
            ProjectInfoManager infoManager = javaExt.getProjectLookup().lookup(ProjectInfoManager.class);
            infoRefRef.compareAndSet(null, infoManager.createInfoRef());
            result = infoRefRef.get();
        }
        return result;
    }

    private ClassPath getPaths(ClassPathType classPathType) {
        ClassPath result = classpaths.get(classPathType);
        if (result == null) {
            result = ClassPathFactory.createClassPath(new GradleClassPaths(classPathType));
        }
        return result;
    }

    public ClassPath getClassPaths(String type) {
        if (ClassPath.SOURCE.equals(type)) {
            return ClassPathSupport.createProxyClassPath(getPaths(ClassPathType.SOURCES), getPaths(ClassPathType.SOURCES_FOR_TEST));
        }
        else if (ClassPath.BOOT.equals(type)) {
            return ClassPathSupport.createProxyClassPath(getPaths(ClassPathType.BOOT), getPaths(ClassPathType.BOOT_FOR_TEST));
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return ClassPathSupport.createProxyClassPath(getPaths(ClassPathType.COMPILE), getPaths(ClassPathType.COMPILE_FOR_TEST));
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return ClassPathSupport.createProxyClassPath(getPaths(ClassPathType.RUNTIME), getPaths(ClassPathType.RUNTIME_FOR_TEST));
        }
        else {
            return ClassPath.EMPTY;
        }
    }

    @Override
    public void onModelChange() {
        if (!hasBeenUsed.get()) {
            return;
        }

        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                loadPathResources(javaExt.getCurrentModel());
            }
        });
    }

    private GradleProperty.BuildPlatform getPlatformProperty() {
        return javaExt.getProjectLookup().lookup(GradleProperty.BuildPlatform.class);
    }

    @Override
    public void onInitProject() {
        final GradleProperty.BuildPlatform platformProperty = getPlatformProperty();

        platformProperty.addChangeListener(new Runnable() {
            @Override
            public void run() {
                currentPlatform = platformProperty.getValue();
                onModelChange();
            }
        });
    }

    // These PropertyChangeListener methods are declared because
    // for some reason, NetBeans want to use them through reflection.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private FileType getTypeOfFile(NbJavaModule module, FileObject file) {
        for (Map.Entry<NbSourceType, NbSourceGroup> entry: module.getSources().entrySet()) {
            for (FileObject sourceRoot: entry.getValue().getFileObjects()) {
                if (FileUtil.getRelativePath(sourceRoot, file) != null) {
                    return sourceTypeToFileType(entry.getKey());
                }
            }
        }

        NbOutput output = module.getProperties().getOutput();

        FileObject outputDir = FileUtil.toFileObject(output.getBuildDir());
        if (outputDir != null && FileUtil.getRelativePath(outputDir, file) != null) {
            return FileType.COMPILED;
        }

        FileObject testOutputDir = FileUtil.toFileObject(output.getTestBuildDir());
        if (testOutputDir != null && FileUtil.getRelativePath(testOutputDir, file) != null) {
            return FileType.COMPILED_TEST;
        }

        return null;
    }

    private static FileType sourceTypeToFileType(NbSourceType sourceType) {
        switch (sourceType) {
            case RESOURCE:
                return FileType.RESOURCE;
            case SOURCE:
                return FileType.SOURCE;
            case TEST_RESOURCE:
                return FileType.TEST_RESOURCE;
            case TEST_SOURCE:
                return FileType.TEST_SOURCE;
            case OTHER:
                return FileType.RESOURCE;
            default:
                throw new AssertionError("Unexpected source type: " + sourceType);
        }
    }

    private FileType getTypeOfFile(NbJavaModel projectModel, FileObject file) {
        NbJavaModule mainModule = projectModel.getMainModule();
        FileType result = getTypeOfFile(mainModule, file);
        if (result != null) {
            return result;
        }
        for (NbJavaModule module: NbJavaModelUtils.getAllModuleDependencies(mainModule)) {
            FileType depResult = getTypeOfFile(module, file);
            if (depResult != null) {
                return depResult;
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
        else if (JavaClassPathConstants.PROCESSOR_PATH.equals(type)) {
            return fileType.isTest()
                    ? ClassPathType.COMPILE_FOR_TEST
                    : ClassPathType.COMPILE;
        }

        return null;
    }

    private ClassPathType getClassPathType(NbJavaModel projectModel, FileObject file, String type) {
        FileType fileType = getTypeOfFile(projectModel, file);
        return getClassPathType(fileType, type);
    }

    public static List<PathResourceImplementation> getPathResources(
            Set<File> invalid, List<File>... fileGroups) {

        int size = 0;
        for (List<?> fileGroup: fileGroups) {
            size += fileGroup.size();
        }

        Set<File> filesSet = new HashSet<File>(4 * size / 3 + 1);
        for (List<File> fileGroup: fileGroups) {
            for (File file: fileGroup) {
                filesSet.add(file);
            }
        }

        List<PathResourceImplementation> result = new ArrayList<PathResourceImplementation>(filesSet.size());
        for (File file: filesSet) {
            URL url = FileUtil.urlForArchiveOrDir(file);

            // Ignore invalid classpath entries
            if (url != null) {
                result.add(ClassPathSupport.createResource(url));
            }
            else {
                invalid.add(file);
                LOGGER.log(Level.WARNING, "Class path entry is invalid: {0}", file);
            }
        }
        return result;
    }

    private void setClassPathResources(
            ClassPathType classPathType,
            List<PathResourceImplementation> paths) {
        classpathResources.put(classPathType, Collections.unmodifiableList(paths));
    }

    private void loadPathResourcesForSources(NbJavaModel projectModel) {
        List<File> sources = new LinkedList<File>();
        List<File> testSources = new LinkedList<File>();

        NbJavaModule mainModule = projectModel.getMainModule();

        sources.addAll(mainModule.getSources(NbSourceType.SOURCE).getFiles());
        testSources.addAll(mainModule.getSources(NbSourceType.TEST_SOURCE).getFiles());

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> sourcePaths = getPathResources(
                new HashSet<File>(), sources);
        setClassPathResources(ClassPathType.SOURCES, sourcePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testSourcePaths = getPathResources(
                new HashSet<File>(), testSources);
        setClassPathResources(ClassPathType.SOURCES_FOR_TEST, testSourcePaths);
    }

    private static void addModuleClassPaths(
            NbJavaModule module,
            List<File> paths,
            List<File> testPaths,
            Collection<File> combinedPaths) {
        NbOutput output = module.getProperties().getOutput();

        if (paths != null) {
            paths.add(output.getBuildDir());
            combinedPaths.add(output.getBuildDir());
        }

        if (testPaths != null) {
            testPaths.add(output.getTestBuildDir());
            combinedPaths.add(output.getTestBuildDir());
        }
    }

    private static void addExternalClassPaths(
            NbUriDependency dependency,
            List<File> paths) {
        File file = dependency.tryGetAsFile();
        if (file != null) {
            paths.add(file);
        }
        else {
            LOGGER.log(Level.WARNING, "Dependency cannot be added to classpath: {0}", dependency.getUri());
        }
    }

    private void loadPathResources(NbJavaModel projectModel) {
        loadPathResourcesForSources(projectModel);

        List<File> compile = new LinkedList<File>();
        List<File> testCompile = new LinkedList<File>();
        List<File> runtime = new LinkedList<File>();
        List<File> testRuntime = new LinkedList<File>();

        // Contains build directories which does not necessarily exists
        Set<File> notRequiredPaths = new HashSet<File>();

        NbJavaModule mainModule = projectModel.getMainModule();
        testCompile.add(mainModule.getProperties().getOutput().getBuildDir());
        testRuntime.add(mainModule.getProperties().getOutput().getBuildDir());

        addModuleClassPaths(mainModule, runtime, testRuntime, notRequiredPaths);

        for (NbJavaDependency dependency: NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.COMPILE)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, compile);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), compile, null, notRequiredPaths);
            }
        }
        for (NbJavaDependency dependency: NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.RUNTIME)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, runtime);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), runtime, null, notRequiredPaths);
            }
        }
        for (NbJavaDependency dependency: NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_COMPILE)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, testCompile);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), testCompile, null, notRequiredPaths);
            }
        }
        for (NbJavaDependency dependency: NbJavaModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_RUNTIME)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, testRuntime);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), testRuntime, null, notRequiredPaths);
            }
        }

        Set<File> missing = new HashSet<File>();

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> compilePaths = getPathResources(missing, compile);
        setClassPathResources(ClassPathType.COMPILE, compilePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testCompilePaths = getPathResources(missing, compile, testCompile);
        setClassPathResources(ClassPathType.COMPILE_FOR_TEST, testCompilePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> runtimePaths = getPathResources(missing, compile, runtime);
        setClassPathResources(ClassPathType.RUNTIME, runtimePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testRuntimePaths = getPathResources(
                missing, compile, testCompile, runtime, testRuntime);
        setClassPathResources(ClassPathType.RUNTIME_FOR_TEST, testRuntimePaths);

        List<PathResourceImplementation> platformResources = new LinkedList<PathResourceImplementation>();
        ProjectPlatform platform = currentPlatform;
        if (platform == null) {
            platform = getPlatformProperty().getValue();
        }
        for (URL url: platform.getBootLibraries()) {
            platformResources.add(ClassPathSupport.createResource(url));
        }

        setClassPathResources(ClassPathType.BOOT, platformResources);
        setClassPathResources(ClassPathType.BOOT_FOR_TEST, platformResources);

        missing.removeAll(notRequiredPaths);

        if (missing.isEmpty()) {
            getInfoRef().setInfo(null);
        }
        else {
            List<ProjectInfo.Entry> infos = new LinkedList<ProjectInfo.Entry>();
            for (File missingDep: missing) {
                infos.add(new ProjectInfo.Entry(ProjectInfo.Kind.WARNING,
                        NbStrings.getInvalidClassPathEntry(missingDep.getPath())));
            }
            getInfoRef().setInfo(new ProjectInfo(infos));
        }

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
        if (GradleFilesClassPathProvider.isGradleFile(file)) {
            return null;
        }

        hasBeenUsed.set(true);

        NbJavaModel projectModel = javaExt.getCurrentModel();
        ClassPathType classPathType = getClassPathType(projectModel, file, type);
        if (classPathType == null) {
            // We don't really know if we will know the classpath of this file
            // or not so we should return ClassPath which returns the classpaths
            // as soon as they become known. However, this means that we never
            // return null (except for gradle files) and this confuses NetBeans,
            // which will cause a problem: The red exclamation mark is not shown
            // on the project node when there is a compile time error in one of
            // the project files.
            //
            // If we need to change our mind and return a delayed ClassPath
            // implementation, it can be found in the commit a997dad9749a222131b4624c5848abf095b766f0.
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
            List<PathResourceImplementation> result = classpathResources.get(classPathType);
            return result != null
                    ? result
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

    private static final class EventSource implements ClassPathImplementation {
        private volatile PropertyChangeSupport changes;

        public void init(PropertyChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            return Collections.emptyList();
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
}
