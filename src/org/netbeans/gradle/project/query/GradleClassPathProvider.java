package org.netbeans.gradle.project.query;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfoRef;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.model.NbDependency;
import org.netbeans.gradle.project.model.NbDependencyType;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbModuleDependency;
import org.netbeans.gradle.project.model.NbOutput;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.model.NbUriDependency;
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
        ProjectInitListener {
    private static final Logger LOGGER = Logger.getLogger(GradleClassPathProvider.class.getName());

    private final NbGradleProject project;
    private final ConcurrentMap<ClassPathType, List<PathResourceImplementation>> classpathResources;
    private final ConcurrentMap<ClassPathType, ClassPath> classpaths;

    private final PropertyChangeSupport changes;
    private volatile JavaPlatform currentPlatform;

    private final AtomicReference<ProjectInfoRef> infoRefRef;
    // This property is used to prevent eagrly loading a project due
    // to changes in the project settings. That is, if this class path provider
    // has never been used, there is no reason to calculate the classpath
    // due to a change in the settings.
    private final AtomicBoolean hasBeenUsed;

    // EnumMap is not a ConcurrentMap, so it cannot be used.
    @SuppressWarnings("MapReplaceableByEnumMap")
    public GradleClassPathProvider(NbGradleProject project) {
        this.project = project;
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
            infoRefRef.compareAndSet(null, project.getProjectInfoManager().createInfoRef());
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
        // Test paths contain all paths
        if (ClassPath.SOURCE.equals(type)) {
            return getPaths(ClassPathType.SOURCES_FOR_TEST);
        }
        else if (ClassPath.BOOT.equals(type)) {
            return getPaths(ClassPathType.BOOT_FOR_TEST);
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return getPaths(ClassPathType.COMPILE_FOR_TEST);
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return getPaths(ClassPathType.RUNTIME_FOR_TEST);
        }
        else {
            return ClassPath.EMPTY;
        }
    }

    private void onModelChange() {
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                loadPathResources(project.getCurrentModel());
            }
        });
    }

    @Override
    public void onInitProject() {
        project.addModelChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onModelChange();
            }
        });
        project.getProperties().getPlatform().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentPlatform = project.getProperties().getPlatform().getValue();
                if (hasBeenUsed.get()) {
                    onModelChange();
                }
            }
        });
        // This is not called because it would trigger the loading of the
        // project even if it just shown in the project open dialog.
        // Although it should be called to ensure correct behaviour in every
        // case.
        // onModelChange();
    }

    // These PropertyChangeListener methods are declared because
    // for some reason, NetBeans want to use them through reflection.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private FileType getTypeOfFile(NbGradleModule module, FileObject file) {
        for (Map.Entry<NbSourceType, NbSourceGroup> entry: module.getSources().entrySet()) {
            for (File sourceRoot: entry.getValue().getPaths()) {
                FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
                if (sourceRootObj != null && FileUtil.getRelativePath(sourceRootObj, file) != null) {
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

    private FileType getTypeOfFile(NbGradleModel projectModel, FileObject file) {
        NbGradleModule mainModule = projectModel.getMainModule();
        FileType result = getTypeOfFile(mainModule, file);
        if (result != null) {
            return result;
        }
        for (NbGradleModule module: NbModelUtils.getAllModuleDependencies(mainModule)) {
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

    private ClassPathType getClassPathType(NbGradleModel projectModel, FileObject file, String type) {
        FileType fileType = getTypeOfFile(projectModel, file);
        return getClassPathType(fileType, type);
    }

    public static List<PathResourceImplementation> getPathResources(
            Set<File> invalid, List<File>... fileGroups) {

        int size = 0;
        for (List<?> fileGroup: fileGroups) {
            size += fileGroup.size();
        }

        List<PathResourceImplementation> result = new ArrayList<PathResourceImplementation>(size);
        for (List<File> fileGroup: fileGroups) {
            for (File file: fileGroup) {
                URL url = FileUtil.urlForArchiveOrDir(file);

                // Ignore non-existent or invalid classpath entries
                if (url != null && file.exists()) {
                    result.add(ClassPathSupport.createResource(url));
                }
                else {
                    invalid.add(file);
                    LOGGER.log(Level.WARNING, "Class path entry does not exists or invalid: {0}", file);
                }
            }
        }
        return result;
    }

    private void setClassPathResources(
            ClassPathType classPathType,
            List<PathResourceImplementation> paths) {
        classpathResources.put(classPathType, Collections.unmodifiableList(paths));
    }

    private void loadPathResourcesForSources(NbGradleModel projectModel) {
        List<File> sources = new LinkedList<File>();
        List<File> testSources = new LinkedList<File>();

        NbGradleModule mainModule = projectModel.getMainModule();

        sources.addAll(mainModule.getSources(NbSourceType.SOURCE).getPaths());
        testSources.addAll(mainModule.getSources(NbSourceType.TEST_SOURCE).getPaths());

        for (NbGradleModule module: NbModelUtils.getAllModuleDependencies(mainModule)) {
            sources.addAll(module.getSources(NbSourceType.SOURCE).getPaths());
        }

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> sourcePaths = getPathResources(
                new HashSet<File>(), sources);
        setClassPathResources(ClassPathType.SOURCES, sourcePaths);

        @SuppressWarnings("unchecked")
        List<PathResourceImplementation> testSourcePaths = getPathResources(
                new HashSet<File>(), sources, testSources);
        setClassPathResources(ClassPathType.SOURCES_FOR_TEST, testSourcePaths);
    }

    private static void addModuleClassPaths(
            NbGradleModule module,
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

    private void loadPathResources(NbGradleModel projectModel) {
        loadPathResourcesForSources(projectModel);

        List<File> compile = new LinkedList<File>();
        List<File> testCompile = new LinkedList<File>();
        List<File> runtime = new LinkedList<File>();
        List<File> testRuntime = new LinkedList<File>();

        // Contains build directories which does not necessarily exists
        Set<File> notRequiredPaths = new HashSet<File>();

        NbGradleModule mainModule = projectModel.getMainModule();
        addModuleClassPaths(mainModule, runtime, testRuntime, notRequiredPaths);

        for (NbDependency dependency: NbModelUtils.getAllDependencies(mainModule, NbDependencyType.COMPILE)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, compile);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), runtime, null, notRequiredPaths);
            }
        }
        for (NbDependency dependency: NbModelUtils.getAllDependencies(mainModule, NbDependencyType.RUNTIME)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, runtime);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), runtime, null, notRequiredPaths);
            }
        }
        for (NbDependency dependency: NbModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_COMPILE)) {
            if (dependency instanceof NbUriDependency) {
                addExternalClassPaths((NbUriDependency)dependency, testCompile);
            }
            else if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                addModuleClassPaths(moduleDep.getModule(), testRuntime, null, notRequiredPaths);
            }
        }
        for (NbDependency dependency: NbModelUtils.getAllDependencies(mainModule, NbDependencyType.TEST_RUNTIME)) {
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

        List<PathResourceImplementation> jdk = new LinkedList<PathResourceImplementation>();
        JavaPlatform platform = currentPlatform;
        if (platform == null) {
            platform = project.getProperties().getPlatform().getValue();
        }
        for (ClassPath.Entry entry: platform.getBootstrapLibraries().entries()) {
            jdk.add(ClassPathSupport.createResource(entry.getURL()));
        }

        setClassPathResources(ClassPathType.BOOT, jdk);
        setClassPathResources(ClassPathType.BOOT_FOR_TEST, jdk);

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

        NbGradleModel projectModel = project.getCurrentModel();
        ClassPathType classPathType = getClassPathType(projectModel, file, type);
        if (classPathType == null) {
            DelayedClassPaths delayedClassPaths = new DelayedClassPaths(file, type);
            return ClassPathFactory.createClassPath(delayedClassPaths);
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

    private class DelayedClassPaths implements ClassPathImplementation {
        private final FileObject file;
        private final String type;

        public DelayedClassPaths(FileObject file, String type) {
            this.file = file;
            this.type = type;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            NbGradleModel projectModel = project.getCurrentModel();
            ClassPathType classPathType = getClassPathType(projectModel, file, type);
            List<PathResourceImplementation> result = classPathType != null
                    ? classpathResources.get(classPathType)
                    : null;
            if (result == null) {
                result = Collections.<PathResourceImplementation>emptyList();
            }
            return result;
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
