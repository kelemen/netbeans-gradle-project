package org.netbeans.gradle.project.java.query;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.utils.LazyValues;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbGradleProject;
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
import org.netbeans.gradle.project.java.model.JavaProjectDependencies;
import org.netbeans.gradle.project.java.model.JavaProjectDependencyDef;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.ProjectDependencyCandidate;
import org.netbeans.gradle.project.java.properties.JavaProjectProperties;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.ClassPathKey;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.ClassPathType;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.ModuleBaseKeys;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.SourceSetClassPathType;
import org.netbeans.gradle.project.java.query.ProjectClassPathResourceBuilder.SpecialClassPath;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.SwingPropertyChangeForwarder;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.DefaultUrlFactory;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.modules.java.api.common.classpath.ClassPathSupportFactory;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.FlaggedClassPathImplementation;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleClassPathProvider
implements
        ClassPathProvider,
        ProjectInitListener,
        JavaModelChangeListener {

    private static final Set<ClassPath.Flag> FLAGS_INCOMPLETE = Collections.unmodifiableSet(EnumSet.of(ClassPath.Flag.INCOMPLETE));

    private final JavaExtension javaExt;
    private final AtomicReference<Map<ClassPathKey, List<PathResourceImplementation>>> classpathResourcesRef;
    private final ConcurrentMap<ClassPathKey, ClassPath> classpaths;

    private final PropertyChangeSupport changes;
    private final AtomicBoolean modelLoadedOnce;
    private final AtomicReference<ProjectPlatform> currentPlatformRef;

    private final Supplier<ProjectIssueRef> infoRefRef;

    private final Supplier<ClassPath> allSourcesClassPathRef;
    private volatile List<PathResourceImplementation> allSources;

    private final AtomicBoolean loadedOnce;

    private final UpdateTaskExecutor classpathUpdateExecutor;
    private final UpdateTaskExecutor changesNotifier;

    private final ListenerRegistrations propertyListenerRefs;

    private final Supplier<ScriptFileProvider> scriptFileProviderRef;

    public GradleClassPathProvider(JavaExtension javaExt) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.currentPlatformRef = new AtomicReference<>(null);
        this.infoRefRef = LazyValues.lazyValue(() -> {
            ProjectIssueManager infoManager = javaExt.getOwnerProjectLookup().lookup(ProjectIssueManager.class);
            return infoManager.createIssueRef();
        });
        this.modelLoadedOnce = new AtomicBoolean(false);
        this.loadedOnce = new AtomicBoolean(false);
        this.scriptFileProviderRef = LazyValues.lazyValue(() -> javaExt.getProject().getLookup().lookup(ScriptFileProvider.class));

        this.classpathResourcesRef = new AtomicReference<>(Collections.<ClassPathKey, List<PathResourceImplementation>>emptyMap());
        this.classpaths = new ConcurrentHashMap<>();
        this.allSources = Collections.emptyList();
        this.allSourcesClassPathRef = LazyValues.lazyValue(() -> {
            return ClassPathFactory.createClassPath(new AllSourcesClassPaths());
        });

        TaskExecutor pathUpdater = TaskExecutors.inOrderSimpleExecutor(NbTaskExecutors.DEFAULT_EXECUTOR);
        this.classpathUpdateExecutor = new GenericUpdateTaskExecutor(pathUpdater);
        this.changesNotifier = SwingExecutors.getSwingUpdateExecutor(true);
        this.propertyListenerRefs = new ListenerRegistrations();

        EventSource eventSource = new EventSource();
        this.changes = new PropertyChangeSupport(eventSource);
        eventSource.init(this.changes);
    }

    private ProjectIssueRef getInfoRef() {
        return infoRefRef.get();
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
                return allSourcesClassPathRef.get();
            case ClassPath.BOOT:
                return getPaths(SpecialClassPath.BOOT);
            case JavaClassPathConstants.MODULE_COMPILE_PATH: /* falls through */
            case ClassPath.COMPILE:
                return getPaths(SpecialClassPath.COMPILE_FOR_GLOBAL);
            case ClassPath.EXECUTE:
                return getPaths(SpecialClassPath.RUNTIME_FOR_GLOBAL);
            default:
                return ClassPath.EMPTY;
        }
    }

    private void scheduleReloadPathResources() {
        classpathUpdateExecutor.execute(() -> {
            loadPathResources(javaExt.getCurrentModel());
        });
    }

    @Override
    public void onModelChange() {
        scheduleReloadPathResources();

        if (javaExt.hasEverBeenLoaded()) {
            if (modelLoadedOnce.compareAndSet(false, true)) {
                tryNotifyFlagsChange();
            }
        }
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

    private GradleProperty.SourceLevel getSourceLevel() {
        return javaExt.getOwnerProjectLookup().lookup(GradleProperty.SourceLevel.class);
    }

    @Override
    public void onInitProject() {
        PropertySource<ProjectPlatform> platformProperty = NbProperties.weakListenerProperty(getPlatformProperty());
        propertyListenerRefs.add(platformProperty.addChangeListener(this::onPlatformChange));

        PropertyReference<Boolean> detectProjectDependenciesByJarNameRef = CommonGlobalSettings.getDefault()
                .detectProjectDependenciesByJarName();
        PropertySource<Boolean> detectProjectDependenciesByJarName
                = NbProperties.weakListenerProperty(PropertyFactory.lazilyNotifiedProperty(detectProjectDependenciesByJarNameRef.getForActiveProfile()));
        propertyListenerRefs.add(detectProjectDependenciesByJarName.addChangeListener(this::scheduleReloadPathResources));

        PropertySource<?> translatedDependencies
                = NbProperties.weakListenerProperty(javaExt.getProjectDependencies().translatedDependencies());
        propertyListenerRefs.add(translatedDependencies.addChangeListener(this::scheduleReloadPathResources));
    }

    // These PropertyChangeListener methods are declared because
    // for some reason, NetBeans want to use them through reflection.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }

    private static boolean isInOneOf(File file, Collection<File> roots) {
        return isInOneOf(file, roots, null);
    }

    private static boolean isInOneOf(File file, Collection<File> roots, ExcludeIncludeRules excludeRules) {
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

    public static JavaSourceSet findAssociatedSourceSet(NbJavaModel projectModel, FileObject fileObj) {
        File file = FileUtil.toFile(fileObj);
        if (file == null) {
            return null;
        }

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
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

        ClassPathType translatedType = translateType(type);
        return translatedType != null
                ? new SourceSetClassPathType(name, translatedType)
                : null;
    }

    private JavaProjectProperties javaProperties() {
        return javaExt.getProjectProperties();
    }

    private ClassPathType translateType(String type) {
        switch (type) {
            case ClassPath.SOURCE:
                return ClassPathType.SOURCES;
            case JavaClassPathConstants.PROCESSOR_PATH: /* falls through */
            case ClassPath.COMPILE:
                return ClassPathType.COMPILE;
            case ClassPath.EXECUTE:
                return ClassPathType.RUNTIME;
            default:
                if (!javaProperties().allowModules().getActiveValue()) {
                    return null;
                }

                switch (type) {
                    case JavaClassPathConstants.MODULE_EXECUTE_PATH:
                        return ClassPathType.MODULE_RUNTIME;
                    case JavaClassPathConstants.MODULE_COMPILE_PATH:
                        return ClassPathType.MODULE_COMPILE;
                    case JavaClassPathConstants.MODULE_BOOT_PATH:
                        return ClassPathType.MODULE_BOOT;
                    default:
                        return null;
                }
        }
    }

    private static void addSourcesOfModule(
            NbJavaModule module,
            List<PathResourceImplementation> result) {

        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                ExcludeIncludeRules includeRules = ExcludeIncludeRules.create(sourceGroup);
                Set<File> sourceRoots = sourceGroup.getSourceRoots();

                result.addAll(getDirPathResources(sourceRoots, new HashSet<>(), includeRules));
            }
        }
    }

    private void updateAllSources(Map<File, JavaProjectDependencyDef> translatedDependencies) {
        NbJavaModel currentModel = javaExt.getCurrentModel();
        NbJavaModule mainModule = currentModel.getMainModule();

        ArrayList<PathResourceImplementation> sources = new ArrayList<>();
        addSourcesOfModule(mainModule, sources);

        for (JavaProjectDependencyDef dependency: translatedDependencies.values()) {
            NbJavaModule module = dependency.getJavaModule();
            addSourcesOfModule(module, sources);
        }

        sources.trimToSize();
        allSources = Collections.unmodifiableList(sources);
    }

    private static List<PathResourceImplementation> getDirPathResources(
            Collection<File> files,
            Set<File> invalid,
            ExcludeIncludeRules includeRules) {
        return ProjectClassPathResourceBuilder.getPathResources(files, invalid, includeRules, DefaultUrlFactory.getDefaultDirFactory());
    }

    private void loadPathResources(NbJavaModel projectModel) {
        JavaProjectDependencies projectDependencies = javaExt.getProjectDependencies();
        projectDependencies.forAllCandidates((ProjectDependencyCandidate candidate) -> {
            NbGradleProject gradleProject = candidate.getProject().getLookup().lookup(NbGradleProject.class);
            if (gradleProject != null) {
                gradleProject.ensureLoadRequested();
            }
        });

        Map<File, JavaProjectDependencyDef> translatedDependencies = projectDependencies
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
            List<ProjectIssue.Entry> infos = new ArrayList<>(missing.size());
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
            changesNotifier.execute(() -> {
                changes.firePropertyChange(ClassPathImplementation.PROP_RESOURCES, null, null);
            });
        }

        if (loadedOnce.compareAndSet(false, true)) {
            tryNotifyFlagsChange();
        }
    }

    private boolean isCompleteClasspath() {
        return loadedOnce.get() && modelLoadedOnce.get();
    }

    private void tryNotifyFlagsChange() {
        if (isCompleteClasspath()) {
            changes.firePropertyChange(FlaggedClassPathImplementation.PROP_FLAGS, null, null);
        }
    }

    private <T> ClassPathSupport.Selector getClassPathSelector(
            PropertySource<? extends T> src,
            Function<? super T, ? extends ClassPath> classPathFactory) {

        SwingPropertyChangeForwarder.Builder sourceLevel = new SwingPropertyChangeForwarder.Builder();
        sourceLevel.addProperty(ClassPathSupport.Selector.PROP_ACTIVE_CLASS_PATH, src);

        SwingPropertyChangeForwarder wrapped = sourceLevel.create();

        return new ClassPathSupport.Selector() {
            @Override
            public ClassPath getActiveClassPath() {
                return classPathFactory.apply(src.getValue());
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {
                wrapped.addPropertyChangeListener(listener);
            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {
                wrapped.removePropertyChangeListener(listener);
            }
        };
    }

    private void loadClassPath(ClassPathKey classPathKey) {
        ModuleBaseKeys moduleBaseKeys = classPathKey.getModuleBaseKeys();
        if (moduleBaseKeys == null) {
            classpaths.putIfAbsent(
                    classPathKey,
                    ClassPathFactory.createClassPath(new GradleClassPaths(classPathKey)));
        }
        else {
            ClassPath base = getClassPath(moduleBaseKeys.getBaseKey());
            ClassPath sources = getClassPath(moduleBaseKeys.getSourcesKey());
            ClassPath boot = getClassPath(SpecialClassPath.BOOT);
            ClassPath legacy = getClassPath(moduleBaseKeys.getLegacyKey());

            ClassPath moduleClassPath = ClassPathFactory.createClassPath(ClassPathSupportFactory
                    .createModuleInfoBasedPath(base, sources, boot, base, legacy, null));

            ClassPathSupport.Selector classPathSelector = getClassPathSelector(getSourceLevel(), sourceLevel -> {
                return GradleSourceLevelQueryImplementation.isModularVersion(sourceLevel) ? moduleClassPath : ClassPath.EMPTY;
            });

            classpaths.putIfAbsent(
                    classPathKey,
                    ClassPathSupport.createMultiplexClassPath(classPathSelector));
        }
    }

    private Map<ClassPathKey, List<PathResourceImplementation>> getClasspathResources() {
        return classpathResourcesRef.get();
    }

    private boolean isScriptFile(FileObject file) {
        ScriptFileProvider scriptProvider = scriptFileProviderRef.get();
        if (scriptProvider == null) {
            return false;
        }

        return scriptProvider.isScriptFileName(file.getNameExt());
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        if (isScriptFile(file)) {
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

        return getClassPath(classPathKey);
    }

    private ClassPath getClassPath(ClassPathKey classPathKey) {
        ClassPath result = classpaths.get(classPathKey);
        if (result != null) {
            return result;
        }

        if (!loadedOnce.get()) {
            loadPathResources(javaExt.getCurrentModel());
        }

        loadClassPath(classPathKey);

        return classpaths.get(classPathKey);
    }

    private class AllSourcesClassPaths implements FlaggedClassPathImplementation {
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

        @Override
        public Set<ClassPath.Flag> getFlags() {
            return isCompleteClasspath()
                    ? Collections.emptySet()
                    : FLAGS_INCOMPLETE;
        }
    }

    private class GradleClassPaths implements FlaggedClassPathImplementation {
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

        @Override
        public Set<ClassPath.Flag> getFlags() {
            return modelLoadedOnce.get()
                    ? Collections.emptySet()
                    : FLAGS_INCOMPLETE;
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
