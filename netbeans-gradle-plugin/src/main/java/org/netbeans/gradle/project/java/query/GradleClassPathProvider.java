package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
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
import org.netbeans.gradle.project.java.model.JavaProjectReference;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.query.GradleFileUtils;
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
    private final ConcurrentMap<ClassPathKey, List<PathResourceImplementation>> classpathResources;
    private final ConcurrentMap<ClassPathKey, ClassPath> classpaths;

    private final PropertyChangeSupport changes;
    private volatile ProjectPlatform currentPlatform;

    private final AtomicReference<ProjectInfoRef> infoRefRef;

    private final AtomicReference<ClassPath> allSourcesClassPathRef;
    private volatile List<PathResourceImplementation> allSources;

    private volatile boolean loadedOnce;

    public GradleClassPathProvider(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");

        this.javaExt = javaExt;
        this.currentPlatform = null;
        this.infoRefRef = new AtomicReference<ProjectInfoRef>(null);
        this.loadedOnce = false;

        this.classpathResources = new ConcurrentHashMap<ClassPathKey, List<PathResourceImplementation>>();
        this.classpaths = new ConcurrentHashMap<ClassPathKey, ClassPath>();
        this.allSources = Collections.emptyList();
        this.allSourcesClassPathRef = new AtomicReference<ClassPath>(null);

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

    private ClassPath getPaths(ClassPathKey classPathType) {
        ClassPath result = classpaths.get(classPathType);
        if (result == null) {
            result = ClassPathFactory.createClassPath(new GradleClassPaths(classPathType));
        }
        return result;
    }

    public ClassPath getAllRuntimeClassPaths() {
        return getPaths(SpecialClassPath.ALL_RUNTIME);
    }

    public ClassPath getBuildOutputClassPaths() {
        return getPaths(SpecialClassPath.ALL_BUILD_OUTPUT);
    }

    public ClassPath getClassPaths(String type) {
        if (ClassPath.SOURCE.equals(type)) {
            ClassPath result = allSourcesClassPathRef.get();
            if (result == null) {
                result = ClassPathFactory.createClassPath(new AllSourcesClassPaths());
                allSourcesClassPathRef.compareAndSet(null, result);
                result = allSourcesClassPathRef.get();
            }
            return result;
        }
        else if (ClassPath.BOOT.equals(type)) {
            return getPaths(SpecialClassPath.BOOT);
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return getPaths(SpecialClassPath.COMPILE_FOR_GLOBAL);
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return getPaths(SpecialClassPath.RUNTIME_FOR_GLOBAL);
        }
        else {
            return ClassPath.EMPTY;
        }
    }

    @Override
    public void onModelChange() {
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

    private boolean isInOneOf(File file, Collection<File> roots) {
        for (File root: roots) {
            if (GradleFileUtils.isParentOrSame(root, file)) {
                return true;
            }
        }
        return false;
    }

    private JavaSourceSet findAssociatedSourceSet(NbJavaModel projectModel, FileObject fileObj) {
        File file = FileUtil.toFile(fileObj);
        if (file == null) {
            return null;
        }

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
            outputDirs.getClassesDir();

            if (isInOneOf(file, sourceSet.getClasspaths().getCompileClasspaths())) {
                return sourceSet;
            }

            if (isInOneOf(file, sourceSet.getClasspaths().getRuntimeClasspaths())) {
                return sourceSet;
            }

            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                if (isInOneOf(file, sourceGroup.getSourceRoots())) {
                    return sourceSet;
                }
            }
        }

        return null;
    }

    private ClassPathKey getClassPathType(NbJavaModel projectModel, FileObject fileObj, String type) {
        JavaSourceSet sourceSet = findAssociatedSourceSet(projectModel, fileObj);
        if (sourceSet == null) {
            return null;
        }

        if (ClassPath.BOOT.equals(type)) {
            return SpecialClassPath.BOOT;
        }

        String name = sourceSet.getName();

        if (ClassPath.SOURCE.equals(type)) {
            return new SourceSetClassPathType(name, ClassPathType.SOURCES);
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return new SourceSetClassPathType(name, ClassPathType.COMPILE);
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return new SourceSetClassPathType(name, ClassPathType.RUNTIME);
        }
        else if (JavaClassPathConstants.PROCESSOR_PATH.equals(type)) {
            return new SourceSetClassPathType(name, ClassPathType.COMPILE);
        }

        LOGGER.log(Level.WARNING, "Unexpected classpath type: {0}", type);
        return null;
    }

    private static void addSourcesOfModule(NbJavaModule module, List<File> sources) {
        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                sources.addAll(sourceGroup.getSourceRoots());
            }
        }
    }

    private void updateAllSources() {
        NbJavaModel currentModel = javaExt.getCurrentModel();
        NbJavaModule mainModule = currentModel.getMainModule();

        List<File> sources = new LinkedList<File>();
        addSourcesOfModule(mainModule, sources);

        for (JavaProjectReference projectRef: currentModel.getAllDependencies()) {
            NbJavaModule module = projectRef.tryGetModule();
            if (module != null) {
                addSourcesOfModule(module, sources);
            }
        }

        List<PathResourceImplementation> sourceContainer = getPathResources(sources, new HashSet<File>());

        allSources = Collections.unmodifiableList(new ArrayList<PathResourceImplementation>(sourceContainer));
    }

    public static List<PathResourceImplementation> getPathResources(Collection<File> files, Set<File> invalid) {
        List<PathResourceImplementation> result = new ArrayList<PathResourceImplementation>(files.size());
        for (File file: new LinkedHashSet<File>(files)) {
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
            ClassPathKey classPathKey,
            List<PathResourceImplementation> paths) {
        classpathResources.put(classPathKey, Collections.unmodifiableList(paths));
    }

    private void loadPathResources(JavaSourceSet sourceSet, Set<File> invalid) {
        Set<File> compileCP = sourceSet.getClasspaths().getCompileClasspaths();
        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.COMPILE),
                getPathResources(compileCP, invalid));

        Set<File> runtimeCP = sourceSet.getClasspaths().getCompileClasspaths();
        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.RUNTIME),
                getPathResources(runtimeCP, invalid));

        List<File> sources = new LinkedList<File>();
        for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
            sources.addAll(sourceGroup.getSourceRoots());
        }

        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.SOURCES),
                getPathResources(sources, invalid));
    }

    private void loadBootClassPath() {
        List<PathResourceImplementation> platformResources = new LinkedList<PathResourceImplementation>();
        ProjectPlatform platform = currentPlatform;
        if (platform == null) {
            platform = getPlatformProperty().getValue();
        }
        for (URL url: platform.getBootLibraries()) {
            platformResources.add(ClassPathSupport.createResource(url));
        }

        setClassPathResources(SpecialClassPath.BOOT, platformResources);
    }

    private void loadAllRuntimeClassPath(NbJavaModule mainModule) {
        Set<File> classPaths = new HashSet<File>();

        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            classPaths.add(sourceSet.getOutputDirs().getClassesDir());
            classPaths.addAll(sourceSet.getClasspaths().getRuntimeClasspaths());
        }

        setClassPathResources(
                SpecialClassPath.ALL_RUNTIME,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private static void removeOtherBuildOutputDirs(NbJavaModel projectModel, Set<File> classPaths) {
        for (JavaProjectReference dependency: projectModel.getAllDependencies()) {
            NbJavaModule module = dependency.tryGetModule();
            if (module != null) {
                for (JavaSourceSet sourceSet: module.getSources()) {
                    classPaths.remove(sourceSet.getOutputDirs().getClassesDir());
                }
            }
        }
    }

    private void loadRuntimeForGlobalClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<File>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.addAll(sourceSet.getClasspaths().getRuntimeClasspaths());
        }

        removeOtherBuildOutputDirs(projectModel, classPaths);

        setClassPathResources(
                SpecialClassPath.RUNTIME_FOR_GLOBAL,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void loadCompileForGlobalClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<File>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.addAll(sourceSet.getClasspaths().getCompileClasspaths());
        }

        removeOtherBuildOutputDirs(projectModel, classPaths);

        setClassPathResources(
                SpecialClassPath.COMPILE_FOR_GLOBAL,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void loadAllBuildOutputClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<File>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.add(sourceSet.getOutputDirs().getClassesDir());
        }

        for (JavaProjectReference dependency: projectModel.getAllDependencies()) {
            NbJavaModule module = dependency.tryGetModule();
            if (module != null) {
                for (JavaSourceSet sourceSet: module.getSources()) {
                    classPaths.add(sourceSet.getOutputDirs().getClassesDir());
                }
            }
        }

        setClassPathResources(
                SpecialClassPath.ALL_BUILD_OUTPUT,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void loadPathResources(NbJavaModel projectModel) {
        // TODO: This method must be called whenever any of the dependent projects
        //   is reloaded.

        Set<File> missing = new HashSet<File>();

        NbJavaModule mainModule = projectModel.getMainModule();
        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            loadPathResources(sourceSet, missing);
        }

        loadBootClassPath();
        loadAllRuntimeClassPath(mainModule);
        loadAllBuildOutputClassPath(projectModel);

        loadCompileForGlobalClassPath(projectModel);
        loadRuntimeForGlobalClassPath(projectModel);

        // TODO: Should we report all invalid?

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

        updateAllSources();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
            }
        });

        loadedOnce = true;
    }

    private void loadClassPath(ClassPathKey classPathKey) {
        classpaths.putIfAbsent(
                classPathKey,
                ClassPathFactory.createClassPath(new GradleClassPaths(classPathKey)));
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        if (GradleFilesClassPathProvider.isGradleFile(file)) {
            return null;
        }

        NbJavaModel projectModel = javaExt.getCurrentModel();
        ClassPathKey classPathKey = getClassPathType(projectModel, file, type);
        if (classPathKey == null) {
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

        ClassPath result = classpaths.get(classPathKey);
        if (result != null) {
            return result;
        }

        if (!loadedOnce) {
            loadPathResources(projectModel);
        }

        loadClassPath(classPathKey);

        return classpaths.get(classPathKey);
    }

    private abstract class AbstractGradleClassPaths implements ClassPathImplementation {
        @Override
        public final void addPropertyChangeListener(PropertyChangeListener listener) {
            changes.addPropertyChangeListener(listener);
        }

        @Override
        public final void removePropertyChangeListener(PropertyChangeListener listener) {
            changes.removePropertyChangeListener(listener);
        }
    }

    private class AllSourcesClassPaths extends AbstractGradleClassPaths {
        @Override
        public List<PathResourceImplementation> getResources() {
            return allSources;
        }
    }

    private class GradleClassPaths extends AbstractGradleClassPaths {
        private final ClassPathKey classPathKey;

        public GradleClassPaths(ClassPathKey classPathKey) {
            assert classPathKey != null;
            this.classPathKey = classPathKey;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            List<PathResourceImplementation> result = classpathResources.get(classPathKey);
            return result != null
                    ? result
                    : Collections.<PathResourceImplementation>emptyList();
        }
    }

    // Just a marker for type safety
    private static interface ClassPathKey {
    }

    private static final class SourceSetClassPathType implements ClassPathKey {
        private final String sourceSetName;
        private final ClassPathType classPathType;

        public SourceSetClassPathType(String sourceSetName, ClassPathType classPathType) {
            assert sourceSetName != null;
            assert classPathType != null;

            this.sourceSetName = sourceSetName;
            this.classPathType = classPathType;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + sourceSetName.hashCode();
            hash = 97 * hash + classPathType.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final SourceSetClassPathType other = (SourceSetClassPathType)obj;

            if (!this.sourceSetName.equals(other.sourceSetName)) return false;
            if (this.classPathType != other.classPathType) return false;
            return true;
        }
    }

    private enum ClassPathType {
        SOURCES,
        COMPILE,
        RUNTIME;
    }

    private enum SpecialClassPath implements ClassPathKey {
        BOOT,
        ALL_RUNTIME,
        ALL_BUILD_OUTPUT,
        COMPILE_FOR_GLOBAL,
        RUNTIME_FOR_GLOBAL,
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
