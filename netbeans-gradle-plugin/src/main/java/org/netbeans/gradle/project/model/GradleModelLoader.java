package org.netbeans.gradle.project.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.util.GradleVersion;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.GradleVersions;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.java.model.NamedFile;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.tasks.DaemonTask;
import org.netbeans.gradle.project.tasks.GradleDaemonFailures;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.gradle.project.view.GlobalErrorReporter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.RequestProcessor;

public final class GradleModelLoader {
    private static final Logger LOGGER = Logger.getLogger(GradleModelLoader.class.getName());

    private static final RequestProcessor PROJECT_LOADER
            = new RequestProcessor("Gradle-Project-Loader", 1, true);

    private static final ModelLoadSupport LISTENERS = new ModelLoadSupport();
    private static final AtomicBoolean CACHE_INIT = new AtomicBoolean(false);

    public static void addModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.addListener(listener);
    }

    public static void removeModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.removeListener(listener);
    }

    private static void updateProjectFromCacheIfNeeded(NbGradleProject project, NbGradleModel baseModel) {
        project.tryUpdateFromCache(baseModel);
    }

    public static NbGradleProject tryFindGradleProject(File projectDir) {
        if (projectDir == null) throw new NullPointerException("projectDir");

        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        return projectDirObj != null
                ? tryFindGradleProject(projectDirObj, projectDir)
                : null;
    }

    private static NbGradleProject tryFindGradleProject(FileObject projectDir, File plainProjectDir) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (plainProjectDir == null) throw new NullPointerException("plainProjectDir");

        Closeable safeToOpen = null;
        try {
            safeToOpen = NbGradleProjectFactory.safeToOpen(plainProjectDir);
            Project project = ProjectManager.getDefault().findProject(projectDir);
            if (project != null) {
                return project.getLookup().lookup(NbGradleProject.class);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to load project: " + projectDir, ex);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Failed to load project: " + projectDir, ex);
        } finally {
            try {
                if (safeToOpen != null) {
                    safeToOpen.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        return null;
    }

    private static GradleModelCache getCache() {
        if (CACHE_INIT.compareAndSet(false, true)) {
            GradleModelCache.getDefault().addModelUpdateListener(new ProjectModelUpdatedListener() {
                @Override
                public void onUpdateProject(NbGradleModel newModel) {
                    NbGradleProject project = tryFindGradleProject(newModel.getProjectDir());
                    if (project != null) {
                        updateProjectFromCacheIfNeeded(project, newModel);
                    }
                }
            });
        }

        return GradleModelCache.getDefault();
    }

    public static GradleConnector createGradleConnector(final Project project) {
        final GradleConnector result = GradleConnector.newConnector();

        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
        }

        File gradleUserHome = GlobalGradleSettings.getGradleUserHomeDir().getValue();
        if (gradleUserHome != null) {
            result.useGradleUserHomeDir(gradleUserHome);
        }

        GradleLocation gradleLocation;
        ProjectProperties projectProperties = gradleProject.tryGetLoadedProperties();
        if (projectProperties == null) {
            LOGGER.warning("Could not wait for retrieving the project properties. Using the globally defined one");
            gradleLocation = GlobalGradleSettings.getGradleHome().getValue();
        }
        else {
            gradleLocation = projectProperties.getGradleLocation().getValue();
        }

        gradleLocation.applyLocation(new GradleLocation.Applier() {
            @Override
            public void applyVersion(String versionStr) {
                result.useGradleVersion(versionStr);
            }

            @Override
            public void applyDirectory(File gradleHome) {
                result.useInstallation(gradleHome);
            }

            @Override
            public void applyDistribution(URI location) {
                result.useDistribution(location);
            }

            @Override
            public void applyDefault() {
            }
        });

        return result;
    }

    private static NbGradleModel tryGetFromCache(File projectDir) {
        File settingsFile = NbGradleModel.findSettingsGradle(projectDir);
        if (settingsFile == null) {
            LOGGER.log(Level.WARNING, "Settings file of the project disappeared: {0}", projectDir);
            return null;
        }

        NbGradleModel result = projectDir != null
                ? getCache().tryGet(projectDir, settingsFile)
                : null;

        return result;
    }

    public static void fetchModel(
            final NbGradleProject project,
            final ModelRetrievedListener listener) {
        fetchModel(project, false, listener);
    }

    public static List<NbGradleExtensionRef> getUnloadedExtensions(
            NbGradleProject project,
            NbGradleModel baseModels) {

        List<NbGradleExtensionRef> result = new LinkedList<NbGradleExtensionRef>();
        for (NbGradleExtensionRef extension: project.getExtensionRefs()) {
            if (!baseModels.hasModelOfExtension(extension)) {
                result.add(extension);
            }
        }
        return result;
    }

    private static boolean hasUnloadedExtension(NbGradleProject project, NbGradleModel cached) {
        for (NbGradleExtensionRef extension: project.getExtensionRefs()) {
            if (!cached.hasModelOfExtension(extension)) {
                return true;
            }
        }
        return false;
    }

    public static void tryUpdateFromCache(
            final NbGradleProject project,
            final NbGradleModel baseModel,
            final ModelRetrievedListener listener) {
        if (project == null) throw new NullPointerException("project");
        if (listener == null) throw new NullPointerException("listener");

        final File projectDir = project.getProjectDirectoryAsFile();
        String caption = NbStrings.getLoadingProjectText(project.getDisplayName());
        GradleDaemonManager.submitGradleTask(PROJECT_LOADER, caption, new DaemonTask() {
            @Override
            public void run(ProgressHandle progress) {
                NbGradleModel model = tryGetFromCache(projectDir);
                if (model == null) {
                    model = baseModel;
                }
                if (model != null) {
                    listener.onComplete(model, null);
                }
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    public static void fetchModel(
            final NbGradleProject project,
            final boolean mayFetchFromCache,
            final ModelRetrievedListener listener) {
        if (project == null) throw new NullPointerException("project");
        if (listener == null) throw new NullPointerException("listener");

        final File projectDir = project.getProjectDirectoryAsFile();
        String caption = NbStrings.getLoadingProjectText(project.getDisplayName());
        GradleDaemonManager.submitGradleTask(PROJECT_LOADER, caption, new DaemonTask() {
            @Override
            public void run(ProgressHandle progress) {
                NbGradleModel model = null;
                Throwable error = null;
                try {
                    if (mayFetchFromCache) {
                        model = tryGetFromCache(projectDir);
                    }
                    if (model == null || hasUnloadedExtension(project, model)) {
                        model = loadModelWithProgress(project, progress, model);
                    }
                } catch (IOException ex) {
                    error = ex;
                } catch (BuildException ex) {
                    error = ex;
                } catch (GradleConnectionException ex) {
                    error = ex;
                } finally {
                    listener.onComplete(model, error);
                    if (error != null) {
                        GradleDaemonFailures.getDefaultHandler().tryHandleFailure(error);
                    }
                }
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    private static JavaPlatform tryGetScriptJavaPlatform(Project project) {
        if (project == null) throw new NullPointerException("project");

        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);

        return gradleProject != null
                ? gradleProject.getProperties().getScriptPlatform().getValue()
                : null;
    }

    private static File getScriptJavaHome(JavaPlatform platform) {
        FileObject jdkHomeObj = platform != null
                ? GlobalGradleSettings.getHomeFolder(platform)
                : null;

        if (jdkHomeObj != null) {
            // This is necessary for unit test code because JavaPlatform returns
            // the jre inside the JDK.
            if ("jre".equals(jdkHomeObj.getNameExt().toLowerCase(Locale.ROOT))) {
                FileObject parent = jdkHomeObj.getParent();
                if (parent != null) {
                    jdkHomeObj = parent;
                }
            }
        }

        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
    }

    public static File getScriptJavaHome(Project project) {
        JavaPlatform platform = tryGetScriptJavaPlatform(project);
        return getScriptJavaHome(platform);
    }

    public static File tryGetModuleDir(IdeaModule module) {
        DomainObjectSet<? extends IdeaContentRoot> contentRoots = module.getContentRoots();
        return contentRoots.isEmpty() ? null : contentRoots.getAt(0).getRootDirectory();
    }

    public static IdeaModule tryFindMainModule(File projectDir, IdeaProject ideaModel) {
        for (IdeaModule module: ideaModel.getModules()) {
            File moduleDir = tryGetModuleDir(module);
            if (moduleDir != null && moduleDir.equals(projectDir)) {
                return module;
            }
        }
        return null;
    }

    private static GradleProject getRoot(GradleProject project) {
        GradleProject prev = null;
        GradleProject current = project;
        do {
            prev = current;
            current = current.getParent();
        } while (current != null);
        return prev;
    }

    public static IdeaModule tryFindRootModule(IdeaProject ideaModel) {
        DomainObjectSet<? extends IdeaModule> modules = ideaModel.getModules();
        if (modules.isEmpty()) {
            return null;
        }

        GradleProject rootProject = getRoot(modules.iterator().next().getGradleProject());
        if (rootProject == null) {
            return null;
        }

        String rootName = rootProject.getPath();

        for (IdeaModule module: ideaModel.getModules()) {
            if (rootName.equals(module.getGradleProject().getPath())) {
                return module;
            }
        }
        return null;
    }

    private static void introduceLoadedModel(NbGradleModel model, boolean replaced) {
        getCache().replaceEntry(model);
        LISTENERS.fireEvent(model);
    }

    public static List<IdeaModule> getChildModules(IdeaModule module) {
        Collection<? extends GradleProject> children = module.getGradleProject().getChildren();
        Set<String> childrenPaths = CollectionUtils.newHashSet(children.size());
        for (GradleProject child: children) {
            childrenPaths.add(child.getPath());
        }

        List<IdeaModule> result = new LinkedList<IdeaModule>();
        for (IdeaModule candidateChild: module.getProject().getModules()) {
            if (childrenPaths.contains(candidateChild.getGradleProject().getPath())) {
                result.add(candidateChild);
            }
        }
        return result;
    }

    private static void introduceProjects(
            List<NbGradleModel> otherModels,
            NbGradleModel mainModel) {

        for (NbGradleModel model: otherModels) {
            introduceLoadedModel(model, false);
        }
        introduceLoadedModel(mainModel, true);
    }

    public static void setupLongRunningOP(OperationInitializer setup, LongRunningOperation op) {
        BuildOperationArgs args = new BuildOperationArgs();
        setup.initOperation(args);
        args.setupLongRunningOP(op);
    }

    private static ModelBuilderSetup modelBuilderSetupImpl(Project project, ProgressHandle progress) {
        return new ModelBuilderSetup(project, progress);
    }

    public static OperationInitializer modelBuilderSetup(Project project, ProgressHandle progress) {
        return modelBuilderSetupImpl(project, progress);
    }

    private static NbGradleModel loadModelWithProgress(
            final NbGradleProject project,
            final ProgressHandle progress,
            final NbGradleModel cachedEntry) throws IOException {
        File projectDir = project.getProjectDirectoryAsFile();

        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        GradleConnector gradleConnector = createGradleConnector(project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;

        NbModelLoader.Result loadedModels;
        try {
            projectConnection = gradleConnector.connect();

            ModelBuilderSetup setup = modelBuilderSetupImpl(project, progress);

            ModelBuilder<BuildEnvironment> modelBuilder = projectConnection.model(BuildEnvironment.class);
            setupLongRunningOP(setup, modelBuilder);

            BuildEnvironment env = modelBuilder.get();
            reportKnownIssues(env);

            GradleTarget gradleTarget = new GradleTarget(
                    setup.getJDKVersion(),
                    GradleVersion.version(env.getGradle().getGradleVersion()));
            NbModelLoader modelLoader = chooseModel(gradleTarget, cachedEntry, setup);

            loadedModels = modelLoader.loadModels(project, projectConnection, progress);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        progress.progress(NbStrings.getParsingModel());

        NbGradleModel result = cachedEntry != null
                ? cachedEntry.updateEntry(loadedModels.getMainModel())
                : loadedModels.getMainModel();

        introduceProjects(loadedModels.getOtherModels(), result);

        return result;
    }

    private static void reportKnownIssues(BuildEnvironment env) {
        GradleVersion version = GradleVersion.version(env.getGradle().getGradleVersion());
        if (GradleVersions.VERSION_1_7.compareTo(version) < 0
                && GradleVersions.VERSION_1_8.compareTo(version) >= 0) {

            GlobalErrorReporter.showIssue(
                    NbStrings.getIssueWithGradle18Message(env.getGradle().getGradleVersion()),
                    null);
        }
    }

    private static NbModelLoader chooseModel(
            GradleTarget gradleTarget,
            NbGradleModel cachedModel,
            OperationInitializer setup) {

        GradleVersion version = gradleTarget.getGradleVersion();

        if (GlobalGradleSettings.getModelLoadingStrategy().getValue().canUse18Api(version)) {
            LOGGER.log(Level.INFO, "Using model loader: {0}", NbGradle18ModelLoader.class.getSimpleName());
            return new NbGradle18ModelLoader(setup, gradleTarget);
        }
        else {
            LOGGER.log(Level.INFO, "Using model loader: {0}", NbCompatibleModelLoader.class.getSimpleName());
            return new NbCompatibleModelLoader(cachedModel, setup, gradleTarget);
        }
    }

    public static NbGradleModel createEmptyModel(File projectDir) {
        return new NbGradleModel(NbGradleMultiProjectDef.createEmpty(projectDir));
    }

    private static <K, V> void addToMap(Map<K, List<V>> map, K key, V value) {
        List<V> valueList = map.get(key);
        if (valueList == null) {
            valueList = new LinkedList<V>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static List<NamedFile> nameSourceRoots(Collection<File> files) {
        // The common case
        if (files.size() == 1) {
            File file = files.iterator().next();
            return Collections.singletonList(new NamedFile(file, file.getName()));
        }

        Map<String, List<FileWithBase>> nameToFile = CollectionUtils.newHashMap(files.size());

        int fileIndex = 0;
        for (File file: files) {
            String name = file.getName();
            File parent = file.getParentFile();
            addToMap(nameToFile, name, new FileWithBase(fileIndex, parent, file));
            fileIndex++;
        }

        boolean didSomething;
        do {
            didSomething = false;

            List<Map.Entry<String, List<FileWithBase>>> currentEntries
                    = new ArrayList<Map.Entry<String, List<FileWithBase>>>(nameToFile.entrySet());
            for (Map.Entry<String, List<FileWithBase>> entry: currentEntries) {
                String entryName = entry.getKey();
                List<FileWithBase> entryFiles = entry.getValue();

                int renameableCount = 0;
                for (FileWithBase file: entryFiles) {
                    if (file.base != null) renameableCount++;
                }

                if (renameableCount > 1) {
                    nameToFile.remove(entryName);
                    for (FileWithBase file: entryFiles) {
                        if (file.base != null) {
                            String newName = file.base.getName() + '/' + entryName;
                            File newParent = file.base.getParentFile();
                            addToMap(nameToFile,
                                    newName,
                                    new FileWithBase(file.index, newParent, file.file));
                        }
                        else {
                            addToMap(nameToFile, entryName, file);
                        }
                    }
                    didSomething = true;
                }
            }
        } while (didSomething);

        NamedFile[] result = new NamedFile[fileIndex];
        for (Map.Entry<String, List<FileWithBase>> entry: nameToFile.entrySet()) {
            String entryName = entry.getKey();
            for (FileWithBase file: entry.getValue()) {
                result[file.index] = new NamedFile(file.file, entryName);
            }
        }

        return Arrays.asList(result);
    }

    private static final class FileWithBase {
        public final int index;
        public final File base;
        public final File file;

        public FileWithBase(int index, File base, File file) {
            assert file != null;

            this.index = index;
            this.base = base;
            this.file = file;
        }
    }

    private GradleModelLoader() {
        throw new AssertionError();
    }

    private static class ModelBuilderSetup implements OperationInitializer {
        private static final SpecificationVersion DEFAULT_JDK_VERSION = new SpecificationVersion("1.5");

        private final ProgressHandle progress;

        private final JavaPlatform jdkPlatform;
        private final File jdkHome;
        private final List<String> globalJvmArgs;

        public ModelBuilderSetup(Project project, ProgressHandle progress) {
            this.progress = progress;

            JavaPlatform selectedPlatform = GradleModelLoader.tryGetScriptJavaPlatform(project);
            this.jdkHome = GradleModelLoader.getScriptJavaHome(selectedPlatform);
            this.jdkPlatform = selectedPlatform != null
                    ? selectedPlatform
                    : JavaPlatform.getDefault();

            List<String> currentJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();
            this.globalJvmArgs = currentJvmArgs != null
                    ? new ArrayList<String>(currentJvmArgs)
                    : Collections.<String>emptyList();
        }

        public JavaPlatform getJdkPlatform() {
            return jdkPlatform;
        }

        public SpecificationVersion getJDKVersion() {
            Specification spec = jdkPlatform.getSpecification();
            if (spec == null) {
                return DEFAULT_JDK_VERSION;
            }

            SpecificationVersion result = spec.getVersion();
            return result != null ? result : DEFAULT_JDK_VERSION;
        }

        @Override
        public void initOperation(BuildOperationArgs args) {
            if (jdkHome != null && !jdkHome.getPath().isEmpty()) {
                args.setJavaHome(jdkHome);
            }

            if (!globalJvmArgs.isEmpty()) {
                args.setJvmArguments(globalJvmArgs.toArray(new String[0]));
            }

            args.setProgressListeners(new ProgressListener[]{
                new ProgressListener() {
                    @Override
                    public void statusChanged(ProgressEvent pe) {
                        progress.progress(pe.getDescription());
                    }
                }
            });
        }
    }
}
