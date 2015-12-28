package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.EventDispatcher;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.GradleVersions;
import org.netbeans.gradle.project.LoadedProjectManager;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;
import org.netbeans.gradle.project.model.issue.ModelLoadIssue;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.model.issue.ModelLoadIssues;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.tasks.DaemonTask;
import org.netbeans.gradle.project.tasks.GradleArguments;
import org.netbeans.gradle.project.tasks.GradleDaemonFailures;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.netbeans.gradle.project.view.GlobalErrorReporter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;

public final class GradleModelLoader {
    private static final Logger LOGGER = Logger.getLogger(GradleModelLoader.class.getName());

    private static final TaskExecutor PROJECT_LOADER
            = NbTaskExecutors.newExecutor("Gradle-Project-Loader", 1);

    private static final MonitorableTaskExecutorService MODEL_LOAD_NOTIFIER
            = NbTaskExecutors.newExecutor("Gradle-Project-Load-Notifier", 1);

    private static final AtomicBoolean CACHE_INIT = new AtomicBoolean(false);

    private static final PersistentModelCache PERSISTENT_CACHE = new MultiFileModelCache();

    private static void updateProjectFromCacheIfNeeded(NbGradleModel newModel) {
        File projectDir = newModel.getProjectDir();
        NbGradleProject project = LoadedProjectManager.getDefault().tryGetLoadedProject(projectDir);
        if (project != null) {
            project.tryReplaceModel(newModel);
        }
    }

    private static GradleModelCache getCache() {
        if (CACHE_INIT.compareAndSet(false, true)) {
            GradleModelCache.getDefault().addModelUpdateListener(new ProjectModelUpdatedListener() {
                @Override
                public void onUpdateProject(NbGradleModel newModel) {
                    updateProjectFromCacheIfNeeded(newModel);
                }
            });
        }

        return GradleModelCache.getDefault();
    }

    private static boolean hasWrapper(NbGradleProject project) {
        Path rootDir = getProjectLoadKey(project).getAppliedRootProjectDir();
        Path wrapperPropertiesFile = rootDir
                .resolve("gradle")
                .resolve("wrapper")
                .resolve("gradle-wrapper.properties");
        return Files.isRegularFile(wrapperPropertiesFile);
    }

    private static boolean shouldRelyOnWrapper(NbGradleProject project, GradleLocationDef locationDef) {
        if (locationDef.getLocation() == GradleLocationDefault.INSTANCE) {
            return true;
        }

        return locationDef.isPreferWrapper() && hasWrapper(project);
    }

    public static GradleConnector createGradleConnector(
            CancellationToken cancelToken,
            final Project project) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(project, "project");

        final GradleConnector result = GradleConnector.newConnector();
        Integer timeoutSec = GlobalGradleSettings.getDefault().gradleDaemonTimeoutSec().getValue();
        if (timeoutSec != null && result instanceof DefaultGradleConnector) {
            ((DefaultGradleConnector)result).daemonMaxIdleTime(timeoutSec, TimeUnit.SECONDS);
        }

        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
        }

        File gradleUserHome = GlobalGradleSettings.getDefault().gradleUserHomeDir().getValue();
        if (gradleUserHome != null) {
            result.useGradleUserHomeDir(gradleUserHome);
        }

        NbGradleCommonProperties commonProperties = gradleProject.getCommonProperties();

        GradleLocationDef gradleLocation = commonProperties.gradleLocation().getActiveValue();
        if (!shouldRelyOnWrapper(gradleProject, gradleLocation)) {
            gradleLocation.getLocation().applyLocation(new GradleLocation.Applier() {
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
        }

        return result;
    }

    private static ProjectLoadRequest getProjectLoadKey(NbGradleProject project) {
        SettingsGradleDef settingsFile = project.getPreferredSettingsGradleDef();
        return new ProjectLoadRequest(project, settingsFile);
    }

    private static NbGradleModel tryGetFromCache(ProjectLoadRequest loadRequest) {
        File settingsFile = loadRequest.findAppliedSettingsFileAsFile();
        return getCache().tryGet(loadRequest.project.getProjectDirectoryAsFile(), settingsFile);
    }

    public static void fetchModel(
            final NbGradleProject project,
            final ModelRetrievedListener listener) {
        fetchModel(project, false, listener);
    }

    public static List<NbGradleExtensionRef> getUnloadedExtensions(
            NbGradleProject project,
            NbGradleModel baseModels) {

        List<NbGradleExtensionRef> result = new LinkedList<>();
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

    private static void onModelLoaded(
            final NbGradleModel model,
            final Throwable error,
            final ModelRetrievedListener listener) {

        if (model == null && error == null) {
            return;
        }

        if (MODEL_LOAD_NOTIFIER.isExecutingInThis()) {
            listener.onComplete(model, error);
        }
        else {
            MODEL_LOAD_NOTIFIER.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    listener.onComplete(model, error);
                }
            }, null);
        }
    }

    public static void tryUpdateFromCache(
            final NbGradleProject project,
            final NbGradleModel baseModel,
            final ModelRetrievedListener listener) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        String caption = NbStrings.getLoadingProjectText(project.displayName().getValue());
        GradleDaemonManager.submitGradleTask(PROJECT_LOADER, caption, new DaemonTask() {
            @Override
            public void run(CancellationToken cancelToken, ProgressHandle progress) {
                NbGradleModel model = tryGetFromCache(getProjectLoadKey(project));
                if (model == null) {
                    model = baseModel;
                }

                onModelLoaded(model, null, listener);
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    private static void reportModelLoadError(NbGradleProject project, GradleModelLoadError error) {
        Throwable unexpectedError = error.getUnexpectedError();
        if (unexpectedError != null) {
            ModelLoadIssue unexpectedIssue = ModelLoadIssues
                    .projectModelLoadError(project, null, null, unexpectedError);
            ModelLoadIssueReporter.reportAllIssues(Collections.singleton(unexpectedIssue));
        }

        Throwable buildScriptEvaluationError = error.getBuildScriptEvaluationError();
        if (buildScriptEvaluationError != null) {
            ModelLoadIssueReporter.reportBuildScriptError(project, buildScriptEvaluationError);
        }
    }

    private static NbGradleModel tryGetFromPersistentCache(ProjectLoadRequest projectLoadKey) {
        try {
            return PERSISTENT_CACHE.tryGetModel(projectLoadKey.project, projectLoadKey.getAppliedRootProjectDir());
        } catch (IOException ex) {
            LOGGER.log(Level.INFO,
                    "Failed to read persistent cache for project " + projectLoadKey.project.getProjectDirectoryAsFile(),
                    ex);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE,
                    "Unexpected error while trying to read the persistent cache for project " + projectLoadKey.project.getProjectDirectoryAsFile(),
                    ex);
        }
        return null;
    }

    public static void fetchModel(
            final NbGradleProject project,
            final boolean mayFetchFromCache,
            final ModelRetrievedListener listener) {

        // TODO: If we already loaded model from the persistent cache for this
        //       project, skip loading from persistent cache.
        if (!mayFetchFromCache || project.hasLoadedProject()) {
            fetchModelWithoutPersistentCache(project, mayFetchFromCache, listener);
            return;
        }

        MODEL_LOAD_NOTIFIER.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                NbGradleModel model = null;
                boolean needLoadFromScripts = true;

                try {
                    ProjectLoadRequest projectLoadKey = getProjectLoadKey(project);
                    model = tryGetFromCache(projectLoadKey);
                    if (model == null || hasUnloadedExtension(project, model)) {
                        if (project.hasLoadedProject()) {
                            model = null;
                        }
                        else {
                            model = tryGetFromPersistentCache(projectLoadKey);
                        }
                    }
                    else {
                        needLoadFromScripts = false;
                    }
                } finally {
                    onModelLoaded(model, null, listener);
                    if (needLoadFromScripts) {
                        fetchModelWithoutPersistentCache(project, mayFetchFromCache, listener);
                    }
                }
            }
        }, null);
    }

    private static boolean isInProjectTree(NbGradleProject project, NbGradleModel rootModel) {
        NbGradleProjectTree projectTree = rootModel.getGenericInfo().getProjectDef().getRootProject();
        return isInProjectTree(project.getProjectDirectoryAsFile(), projectTree);
    }

    private static boolean isInProjectTree(File projectDir, NbGradleProjectTree projectTree) {
        if (Objects.equals(projectDir, projectTree.getProjectDir())) {
            return true;
        }

        for (NbGradleProjectTree child: projectTree.getChildren()) {
            if (isInProjectTree(projectDir, child)) {
                return true;
            }
        }
        return false;
    }

    private static ProjectLoadRequest fixProjectLoadKey(
            CancellationToken cancelToken,
            NbGradleProject project,
            ProjectLoadRequest projectLoadKey,
            ProgressHandle progress) throws IOException, GradleModelLoadError {
        if (!GlobalGradleSettings.getDefault().loadRootProjectFirst().getValue()) {
            return projectLoadKey;
        }

        Path rootProjectDir = projectLoadKey.getAppliedRootProjectDir();
        NbGradleProject rootProject = NbGradleProjectFactory.tryLoadSafeGradleProject(rootProjectDir);
        if (rootProject == null) {
            LOGGER.log(Level.INFO, "Failed to load root project for {0}.", project.getProjectDirectoryAsPath());
            return projectLoadKey;
        }

        ProjectLoadRequest rootLoadKey = new ProjectLoadRequest(rootProject, projectLoadKey.settingsGradleDef);
        NbGradleModel rootModel = tryGetFromCache(rootLoadKey);
        if (rootModel == null) {
            rootModel = loadModelWithProgress(cancelToken, rootLoadKey, progress, null);
            assert rootModel != null;
        }

        if (!isInProjectTree(project, rootModel)) {
            LOGGER.log(Level.INFO, "Project ({0}) is not found in the project tree of {1}. Assuming the project to be a single separate project.",
                    new Object[]{project.getProjectDirectoryAsFile(), rootModel.getProjectDir()});
            return new ProjectLoadRequest(project, SettingsGradleDef.NO_SETTINGS);
        }

        return projectLoadKey;
    }

    private static void fetchModelWithoutPersistentCache(
            final NbGradleProject project,
            final boolean mayFetchFromCache,
            final ModelRetrievedListener listener) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        String caption = NbStrings.getLoadingProjectText(project.displayName().getValue());
        GradleDaemonManager.submitGradleTask(PROJECT_LOADER, caption, new DaemonTask() {
            @Override
            public void run(CancellationToken cancelToken, ProgressHandle progress) {
                ProjectLoadRequest projectLoadKey = getProjectLoadKey(project);

                NbGradleModel model = null;
                Throwable error = null;
                try {
                    ProjectLoadRequest fixedLoadKey = fixProjectLoadKey(cancelToken, project, projectLoadKey, progress);
                    if (mayFetchFromCache) {
                        model = tryGetFromCache(fixedLoadKey);
                    }
                    if (model == null || hasUnloadedExtension(project, model)) {
                        model = loadModelWithProgress(cancelToken, fixedLoadKey, progress, model);
                    }
                } catch (IOException | BuildException ex) {
                    error = ex;
                } catch (GradleConnectionException ex) {
                    error = ex;
                } catch (GradleModelLoadError ex) {
                    error = ex;
                    reportModelLoadError(project, ex);
                } finally {
                    onModelLoaded(model, error, listener);

                    if (error != null) {
                        GradleDaemonFailures.getDefaultHandler().tryHandleFailure(error);
                    }
                }
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    private static JavaPlatform tryGetScriptJavaPlatform(Project project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);

        return gradleProject != null
                ? gradleProject.getCommonProperties().scriptPlatform().getActiveValue()
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

    private static void saveToPersistentCache(Collection<NbGradleModel> models) {
        try {
            PERSISTENT_CACHE.saveGradleModels(models);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed to save into the persistent cache.", ex);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error while saving to the persistent cache.", ex);
        }
    }

    private static NbGradleModel introduceLoadedModel(NbGradleModel model, boolean replaced) {
        NbGradleModel modelToSave;
        if (replaced) {
            modelToSave = model;
            getCache().replaceEntry(model);
        }
        else {
            modelToSave = getCache().updateEntry(model);
        }

        NbGradleProject ownerProject = LoadedProjectManager.getDefault().tryGetLoadedProject(model.getProjectDir());
        if (ownerProject != null) {
            ownerProject.tryReplaceModel(modelToSave);
        }

        return modelToSave;
    }

    private static void introduceProjects(
            List<NbGradleModel> otherModels,
            NbGradleModel mainModel) {

        List<NbGradleModel> toSave = new ArrayList<>(otherModels.size() + 1);
        for (NbGradleModel model: otherModels) {
            toSave.add(introduceLoadedModel(model, false));
        }
        toSave.add(introduceLoadedModel(mainModel, true));
        saveToPersistentCache(toSave);
    }

    public static void setupLongRunningOP(OperationInitializer setup, LongRunningOperation op) {
        BuildOperationArgs args = new BuildOperationArgs();
        setup.initOperation(args);
        args.setupLongRunningOP(op);
    }

    private static DaemonTaskContext daemonTaskContext(Project project) {
        return new DaemonTaskContext(project, true);
    }

    private static List<String> getModelEvaluateJvmArguments(Project project) {
        return GradleArguments.getExtraJvmArgs(daemonTaskContext(project));
    }

    private static ModelBuilderSetup modelBuilderSetup(ProjectLoadRequest projectLoadKey, ProgressHandle progress) {
        return new ModelBuilderSetup(
                projectLoadKey.project,
                getModelEvaluateArguments(projectLoadKey),
                getModelEvaluateJvmArguments(projectLoadKey.project),
                progress);
    }

    public static ModelBuilderSetup modelBuilderSetup(NbGradleProject project, ProgressHandle progress) {
        return modelBuilderSetup(getProjectLoadKey(project), progress);
    }

    public static ModelBuilderSetup modelBuilderSetup(Project project, ProgressHandle progress) {
        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject != null) {
            return modelBuilderSetup(gradleProject, progress);
        }

        // This path should not be taken under normal circumstances.
        // That is, this path is only taken if for some weird reasons a non-gradle
        // project is being interpreted as a Gradle project.
        return new ModelBuilderSetup(
                project,
                getModelEvaluateArguments(project, SettingsGradleDef.DEFAULT),
                getModelEvaluateJvmArguments(project),
                progress);
    }

    private static NbGradleModel loadModelWithProgress(
            CancellationToken cancelToken,
            final ProjectLoadRequest projectLoadKey,
            final ProgressHandle progress,
            final NbGradleModel cachedEntry) throws IOException, GradleModelLoadError {

        NbGradleProject project = projectLoadKey.project;
        File projectDir = project.getProjectDirectoryAsFile();

        LOGGER.log(Level.INFO,
                "Loading Gradle project from directory: {0}, settings.gradle: {1}",
                new Object[]{projectDir, projectLoadKey.settingsGradleDef});

        GradleConnector gradleConnector = createGradleConnector(cancelToken, project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;

        NbModelLoader.Result loadedModels;
        try {
            projectConnection = gradleConnector.connect();

            ModelBuilderSetup setup = modelBuilderSetup(projectLoadKey, progress);

            ModelBuilder<BuildEnvironment> modelBuilder = projectConnection.model(BuildEnvironment.class);
            setupLongRunningOP(setup, modelBuilder);

            BuildEnvironment env = modelBuilder.get();
            reportKnownIssues(env);

            GradleTarget gradleTarget = new GradleTarget(
                    setup.getJDKVersion(),
                    GradleVersion.version(env.getGradle().getGradleVersion()));
            NbModelLoader modelLoader = chooseModel(projectLoadKey.settingsGradleDef, gradleTarget, cachedEntry, setup);

            loadedModels = modelLoader.loadModels(project, projectConnection, progress);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        ModelLoadIssueReporter.reportAllIssues(loadedModels.getIssues());

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

            String gradleVersion = env.getGradle().getGradleVersion();
            GlobalErrorReporter.showIssue(NbStrings.getIssueWithGradle18Message(gradleVersion));
        }
        else if (GradleVersions.VERSION_2_3.equals(version.getBaseVersion())) {
            String gradleVersion = env.getGradle().getGradleVersion();
            GlobalErrorReporter.showIssue(NbStrings.getIssueWithGradle23Message(gradleVersion));
        }
    }

    private static NbModelLoader chooseModel(
            SettingsGradleDef settingsGradleDef,
            GradleTarget gradleTarget,
            NbGradleModel cachedModel,
            OperationInitializer setup) {

        GradleVersion version = gradleTarget.getGradleVersion();

        ModelLoadingStrategy modelLoadingStrategy = GlobalGradleSettings.getDefault().modelLoadingStrategy().getValue();
        NbModelLoader result = modelLoadingStrategy.canUse18Api(version)
                ? new NbGradle18ModelLoader(settingsGradleDef, setup, gradleTarget)
                : new NbCompatibleModelLoader(settingsGradleDef, cachedModel, setup, gradleTarget);

        LOGGER.log(Level.INFO, "Using model loader: {0}", result.getClass().getSimpleName());
        return result;
    }

    public static NbGradleModel createEmptyModel(File projectDir) {
        return new NbGradleModel(NbGradleMultiProjectDef.createEmpty(projectDir));
    }

    private static List<String> getModelEvaluateArguments(Project project, SettingsGradleDef settingsDef) {
        return GradleArguments.getExtraArgs(settingsDef, daemonTaskContext(project));
    }

    private static List<String> getModelEvaluateArguments(ProjectLoadRequest projectLoadKey) {
        return GradleArguments.getExtraArgs(
                projectLoadKey.settingsGradleDef,
                daemonTaskContext(projectLoadKey.project));
    }

    private static final class ProjectLoadRequest {
        public final NbGradleProject project;
        public final SettingsGradleDef settingsGradleDef;

        public ProjectLoadRequest(NbGradleProject project, SettingsGradleDef settingsGradleDef) {
            assert project != null;
            assert settingsGradleDef != null;

            this.project = project;
            this.settingsGradleDef = settingsGradleDef;
        }

        public File findAppliedSettingsFileAsFile() {
            Path result = findAppliedSettingsFile();
            return result != null ? result.toFile() : null;
        }

        public Path findAppliedSettingsFile() {
            Path settingsFile = settingsGradleDef.getSettingsGradle();

            if (settingsFile != null) {
                return settingsFile;
            }

            return NbGradleModel.findSettingsGradle(project.getProjectDirectoryAsFile());
        }

        public Path getAppliedRootProjectDir() {
            Path appliedSettingsFile = settingsGradleDef.getSettingsGradle();
            if (appliedSettingsFile == null) {
                appliedSettingsFile = findAppliedSettingsFile();
            }

            if (appliedSettingsFile != null) {
                Path settingsFileDir = appliedSettingsFile.getParent();
                if (settingsFileDir != null) {
                    return settingsFileDir;
                }
            }

            return project.getProjectDirectoryAsPath();
        }
    }

    public static class ModelBuilderSetup implements OperationInitializer {
        private static final SpecificationVersion DEFAULT_JDK_VERSION = new SpecificationVersion("1.5");

        private final ProgressHandle progress;

        private final JavaPlatform jdkPlatform;
        private final File jdkHome;
        private final List<String> arguments;
        private final List<String> jvmArgs;

        public ModelBuilderSetup(
                Project project,
                List<String> arguments,
                List<String> jvmArgs,
                ProgressHandle progress) {
            this.progress = progress;

            JavaPlatform selectedPlatform = GradleModelLoader.tryGetScriptJavaPlatform(project);
            this.jdkHome = GradleModelLoader.getScriptJavaHome(selectedPlatform);
            this.jdkPlatform = selectedPlatform != null
                    ? selectedPlatform
                    : JavaPlatform.getDefault();

            this.arguments = arguments != null
                    ? new ArrayList<>(arguments)
                    : Collections.<String>emptyList();
            this.jvmArgs = jvmArgs != null
                    ? new ArrayList<>(jvmArgs)
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

            if (!arguments.isEmpty()) {
                args.setArguments(arguments.toArray(new String[arguments.size()]));
            }

            if (!jvmArgs.isEmpty()) {
                args.setJvmArguments(jvmArgs.toArray(new String[jvmArgs.size()]));
            }

            if (progress != null) {
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

    private GradleModelLoader() {
        throw new AssertionError();
    }

    private enum ModelLoaderDispatcher implements EventDispatcher<ModelLoadListener, NbGradleModel> {
        INSTANCE;

        @Override
        public void onEvent(ModelLoadListener eventListener, NbGradleModel arg) {
            eventListener.modelLoaded(arg);
        }
    }
}
