package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.ProjectIssue;
import org.netbeans.gradle.project.ProjectIssueManager;
import org.netbeans.gradle.project.ProjectIssueRef;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.JavaModelChangeListener;
import org.netbeans.gradle.project.java.model.JavaProjectDependencyDef;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.ProjectDependencyCandidate;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.ClassPathKey;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.ClassPathType;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.SourceSetClassPathType;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.SpecialClassPath;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleClassPathProvider
implements
        ClassPathProvider,
        ProjectInitListener,
        JavaModelChangeListener {

    private final JavaExtension javaExt;
    private final AtomicReference<Map<ClassPathKey, List<PathResourceImplementation>>> classpathResourcesRef;
    private final ConcurrentMap<ClassPathKey, ClassPath> classpaths;

    private final PropertyChangeSupport changes;
    private final AtomicReference<ProjectPlatform> currentPlatformRef;

    private final AtomicReference<ProjectIssueRef> infoRefRef;

    private final AtomicReference<ClassPath> allSourcesClassPathRef;
    private volatile List<PathResourceImplementation> allSources;

    private volatile boolean loadedOnce;

    private final UpdateTaskExecutor classpathUpdateExecutor;

    private final ListenerRegistrations propertyListenerRefs;

    public GradleClassPathProvider(JavaExtension javaExt) {
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");

        this.javaExt = javaExt;
        this.currentPlatformRef = new AtomicReference<>(null);
        this.infoRefRef = new AtomicReference<>(null);
        this.loadedOnce = false;

        this.classpathResourcesRef = new AtomicReference<>(Collections.<ClassPathKey, List<PathResourceImplementation>>emptyMap());
        this.classpaths = new ConcurrentHashMap<>();
        this.allSources = Collections.emptyList();
        this.allSourcesClassPathRef = new AtomicReference<>(null);

        TaskExecutor pathUpdater = TaskExecutors.inOrderSimpleExecutor(NbTaskExecutors.DEFAULT_EXECUTOR);
        this.classpathUpdateExecutor = new GenericUpdateTaskExecutor(pathUpdater);
        this.propertyListenerRefs = new ListenerRegistrations();

        EventSource eventSource = new EventSource();
        this.changes = new PropertyChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    private ProjectIssueRef getInfoRef() {
        ProjectIssueRef result = infoRefRef.get();
        if (result == null) {
            ProjectIssueManager infoManager = javaExt.getOwnerProjectLookup().lookup(ProjectIssueManager.class);
            infoRefRef.compareAndSet(null, infoManager.createIssueRef());
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
        if (type == null) {
            return ClassPath.EMPTY;
        }

        switch (type) {
            case ClassPath.SOURCE:
                ClassPath result = allSourcesClassPathRef.get();
                if (result == null) {
                    result = ClassPathFactory.createClassPath(new AllSourcesClassPaths());
                    allSourcesClassPathRef.compareAndSet(null, result);
                    result = allSourcesClassPathRef.get();
                }
                return result;
            case ClassPath.BOOT:
                return getPaths(SpecialClassPath.BOOT);
            case ClassPath.COMPILE:
                return getPaths(SpecialClassPath.COMPILE_FOR_GLOBAL);
            case ClassPath.EXECUTE:
                return getPaths(SpecialClassPath.RUNTIME_FOR_GLOBAL);
            default:
                return ClassPath.EMPTY;
        }
    }

    private void scheduleReloadPathResources() {
        classpathUpdateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                loadPathResources(javaExt.getCurrentModel());
            }
        });
    }

    @Override
    public void onModelChange() {
        scheduleReloadPathResources();
    }

    private ProjectPlatform getCurrentPlatform() {
        ProjectPlatform result = currentPlatformRef.get();
        if (result == null) {
            result = getPlatformProperty().getValue();
            if (!currentPlatformRef.compareAndSet(null, result)) {
                result = currentPlatformRef.get();
            }
        }
        return result;
    }

    private void onPlatformChange() {
        currentPlatformRef.set(getPlatformProperty().getValue());
        scheduleReloadPathResources();
    }

    private GradleProperty.BuildPlatform getPlatformProperty() {
        return javaExt.getOwnerProjectLookup().lookup(GradleProperty.BuildPlatform.class);
    }

    @Override
    public void onInitProject() {
        PropertySource<ProjectPlatform> platformProperty = NbProperties.weakListenerProperty(getPlatformProperty());
        propertyListenerRefs.add(platformProperty.addChangeListener(new Runnable() {
            @Override
            public void run() {
                onPlatformChange();
            }
        }));

        PropertyReference<Boolean> detectProjectDependenciesByJarNameRef = CommonGlobalSettings.getDefault()
                .detectProjectDependenciesByJarName();
        PropertySource<Boolean> detectProjectDependenciesByJarName
                = NbProperties.weakListenerProperty(PropertyFactory.lazilyNotifiedProperty(detectProjectDependenciesByJarNameRef.getForActiveProfile()));
        propertyListenerRefs.add(detectProjectDependenciesByJarName.addChangeListener(new Runnable() {
            @Override
            public void run() {
                scheduleReloadPathResources();
            }
        }));

        PropertySource<Map<File, ProjectDependencyCandidate>> translatedDependencies
                = NbProperties.weakListenerProperty(javaExt.getProjectDependencies().translatedDependencies());
        propertyListenerRefs.add(translatedDependencies.addChangeListener(new Runnable() {
            @Override
            public void run() {
                scheduleReloadPathResources();
            }
        }));
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
        return isInOneOf(file, roots, null);
    }

    private boolean isInOneOf(File file, Collection<File> roots, ExcludeIncludeRules excludeRules) {
        for (File root: roots) {
            if (NbFileUtils.isParentOrSame(root, file)) {
                if (excludeRules == null) {
                    return true;
                }
                return excludeRules.isIncluded(root.toPath(), file);
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
                ExcludeIncludeRules excludeRules = ExcludeIncludeRules.create(sourceGroup);
                if (isInOneOf(file, sourceGroup.getSourceRoots(), excludeRules)) {
                    return sourceSet;
                }
            }
        }

        return null;
    }

    private ClassPathKey getClassPathType(NbJavaModel projectModel, FileObject fileObj, String type) {
        if (type == null) {
            return null;
        }

        JavaSourceSet sourceSet = findAssociatedSourceSet(projectModel, fileObj);
        if (sourceSet == null) {
            return null;
        }

        if (ClassPath.BOOT.equals(type)) {
            return SpecialClassPath.BOOT;
        }

        String name = sourceSet.getName();

        switch (type) {
            case ClassPath.SOURCE:
                return new SourceSetClassPathType(name, ClassPathType.SOURCES);
            case JavaClassPathConstants.PROCESSOR_PATH: /* falls through */
            case ClassPath.COMPILE:
                return new SourceSetClassPathType(name, ClassPathType.COMPILE);
            case ClassPath.EXECUTE:
                return new SourceSetClassPathType(name, ClassPathType.RUNTIME);
            default:
                return null;
        }
    }

    private static void addSourcesOfModule(
            NbJavaModule module,
            List<PathResourceImplementation> result) {

        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                ExcludeIncludeRules includeRules = ExcludeIncludeRules.create(sourceGroup);
                Set<File> sourceRoots = sourceGroup.getSourceRoots();

                result.addAll(getPathResources(sourceRoots, new HashSet<File>(), includeRules));
            }
        }
    }

    private void updateAllSources(Map<File, ProjectDependencyCandidate> translatedDependencies) {
        NbJavaModel currentModel = javaExt.getCurrentModel();
        NbJavaModule mainModule = currentModel.getMainModule();

        List<PathResourceImplementation> sources = new LinkedList<>();
        addSourcesOfModule(mainModule, sources);

        for (ProjectDependencyCandidate candidate: translatedDependencies.values()) {
            JavaProjectDependencyDef dependency = candidate.tryGetDependency();
            if (dependency != null) {
                NbJavaModule module = dependency.getJavaModule();
                addSourcesOfModule(module, sources);
            }
        }

        allSources = Collections.unmodifiableList(new ArrayList<>(sources));
    }

    public static List<PathResourceImplementation> getPathResources(
            Collection<File> files,
            Set<File> invalid,
            ExcludeIncludeRules includeRules) {
        return ProjectClassPathResourceBuilder.getPathResources(files, invalid, includeRules);
    }

    private void loadPathResources(NbJavaModel projectModel) {
        Map<File, ProjectDependencyCandidate> translatedDependencies = javaExt
                .getProjectDependencies()
                .translatedDependencies()
                .getValue();
        ProjectClassPathResourceBuilder builder = new ProjectClassPathResourceBuilder(
                projectModel, translatedDependencies, getCurrentPlatform());
        builder.build();

        Map<ClassPathKey, List<PathResourceImplementation>> newClasspathResources = builder.getClasspathResources();
        Map<ClassPathKey, List<PathResourceImplementation>> prevClasspathResources = classpathResourcesRef.getAndSet(newClasspathResources);

        Set<File> missing = builder.getMissing();

        // TODO: Should we report all invalid?

        if (missing.isEmpty()) {
            getInfoRef().setInfo(null);
        }
        else {
            List<ProjectIssue.Entry> infos = new LinkedList<>();
            for (File missingDep: missing) {
                infos.add(new ProjectIssue.Entry(ProjectIssue.Kind.WARNING,
                        NbStrings.getInvalidClassPathEntryTitle(),
                        NbStrings.getInvalidClassPathEntry(missingDep.getPath())));
            }
            getInfoRef().setInfo(new ProjectIssue(infos));
        }

        updateAllSources(translatedDependencies);

        boolean changed = !prevClasspathResources.equals(newClasspathResources);
        if (changed) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
                }
            });
        }

        loadedOnce = true;
    }

    private void loadClassPath(ClassPathKey classPathKey) {
        classpaths.putIfAbsent(
                classPathKey,
                ClassPathFactory.createClassPath(new GradleClassPaths(classPathKey)));
    }

    private Map<ClassPathKey, List<PathResourceImplementation>> getClasspathResources() {
        return classpathResourcesRef.get();
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

    private class AllSourcesClassPaths implements ClassPathImplementation {
        @Override
        public List<PathResourceImplementation> getResources() {
            return allSources;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changes.addPropertyChangeListener(listener);
        }

        @Override
        public final void removePropertyChangeListener(PropertyChangeListener listener) {
            changes.removePropertyChangeListener(listener);
        }
    }

    private class GradleClassPaths implements ClassPathImplementation {
        private final ClassPathKey classPathKey;

        public GradleClassPaths(ClassPathKey classPathKey) {
            assert classPathKey != null;
            this.classPathKey = classPathKey;
        }

        @Override
        public List<PathResourceImplementation> getResources() {
            List<PathResourceImplementation> result = getClasspathResources().get(classPathKey);
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
