package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.tasks.DaemonTask;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

public final class GradleModelLoader {
    private static final Logger LOGGER = Logger.getLogger(GradleModelLoader.class.getName());

    private static final RequestProcessor PROJECT_LOADER
            = new RequestProcessor("Gradle-Project-Loader", 1, true);

    private static GradleModelCache CACHE = new GradleModelCache(100);
    private static ModelLoadSupport LISTENERS = new ModelLoadSupport();

    static {
        GlobalGradleSettings.getProjectCacheSize().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                CACHE.setMaxCapacity(GlobalGradleSettings.getProjectCacheSize().getValue());
            }
        });
    }

    public static void addModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.addListener(listener);
    }

    public static void removeModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.removeListener(listener);
    }

    public static GradleConnector createGradleConnector(final NbGradleProject project) {
        final GradleConnector result = GradleConnector.newConnector();

        File gradleUserHome = GlobalGradleSettings.getGradleUserHomeDir().getValue();
        if (gradleUserHome != null) {
            result.useGradleUserHomeDir(gradleUserHome);
        }

        GradleLocation gradleLocation = project.getProperties().getGradleLocation().getValue();

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
            LOGGER.log(Level.WARNING, "Settings file of the project disappeared: {0}", settingsFile);
            return null;
        }

        NbGradleModel result = projectDir != null
                ? CACHE.tryGet(projectDir, settingsFile)
                : null;

        if (result != null && result.isDirty()) {
            result = null;
        }
        return result;
    }

    public static void fetchModel(
            final NbGradleProject project,
            final ModelRetrievedListener listener) {
        fetchModel(project, false, listener);
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
                    if (model == null) {
                        model = loadModelWithProgress(project, progress);
                    }
                } catch (IOException ex) {
                    error = ex;
                } catch (BuildException ex) {
                    error = ex;
                } catch (GradleConnectionException ex) {
                    error = ex;
                } finally {
                    listener.onComplete(model, error);
                }
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    private static NbOutput createDefaultOutput(File projectDir) {
        File buildDir = new File(projectDir, "build" + File.separatorChar + "classes");

        return new NbOutput(
                new File(buildDir, "main"),
                new File(buildDir, "test"));
    }

    public static NbGradleModel createEmptyModel(FileObject projectDir) throws IOException {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IllegalStateException("Project directory does not exist.");
        }
        return createEmptyModel(projectDirAsFile, Lookup.EMPTY);
    }

    private static NbGradleModel createEmptyModel(File projectDir, Lookup otherModels) throws IOException {
        String name = projectDir.getName();

        String level = AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                name,
                name,
                projectDir,
                createDefaultOutput(projectDir),
                level,
                level,
                Collections.<NbGradleTask>emptyList());

        NbGradleModule mainModule = new NbGradleModule(properties,
                Collections.<NbSourceType, NbSourceGroup>emptyMap(),
                Collections.<File>emptyList(),
                Collections.<NbDependencyType, NbDependencyGroup>emptyMap(),
                Collections.<NbGradleModule>emptyList());

        return new NbGradleModel(projectDir, mainModule, otherModels);
    }

    public static File getScriptJavaHome(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        JavaPlatform platform = project.getProperties().getScriptPlatform().getValue();
        FileObject jdkHomeObj = platform != null
                ? GlobalGradleSettings.getHomeFolder(platform)
                : null;
        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
    }

    private static Object getRawModelWithProgress(
            NbGradleProject project,
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<?> model) {
        // Actually this cast is only needed to be complile against pre 1.6
        // Tooling API because from version 1.6 there is no need to extend
        // Model.
        // TODO: Remove this hack after upgrading to 1.6
        @SuppressWarnings("unchecked")
        Class<? extends Model> castedModel = (Class<? extends Model>)model;
        return getModelWithProgress(project, progress, projectConnection, castedModel);
    }

    private static <T extends Model> T getModelWithProgress(
            NbGradleProject project,
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<? extends T> model) {
        ModelBuilder<? extends T> builder = projectConnection.model(model);

        File jdkHome = getScriptJavaHome(project);
        if (jdkHome != null && !jdkHome.getPath().isEmpty()) {
            builder.setJavaHome(jdkHome);
        }

        List<String> globalJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();

        if (globalJvmArgs != null && !globalJvmArgs.isEmpty()) {
            builder.setJvmArguments(globalJvmArgs.toArray(new String[0]));
        }

        builder.addProgressListener(new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent pe) {
                progress.progress(pe.getDescription());
            }
        });

        return builder.get();
    }

    private static File tryGetModuleDir(IdeaModule module) {
        DomainObjectSet<? extends IdeaContentRoot> contentRoots = module.getContentRoots();
        return contentRoots.isEmpty() ? null : contentRoots.getAt(0).getRootDirectory();
    }

    private static IdeaModule tryFindMainModule(File projectDir, IdeaProject ideaModel) {
        for (IdeaModule module: ideaModel.getModules()) {
            File moduleDir = tryGetModuleDir(module);
            if (moduleDir != null && moduleDir.equals(projectDir)) {
                return module;
            }
        }
        return null;
    }

    private static Map<NbDependencyType, NbDependencyGroup> getDependencies(
            IdeaModule module, Map<String, NbGradleModule> parsedModules) {

        DependencyBuilder dependencies = new DependencyBuilder();

        for (IdeaDependency dependency: module.getDependencies()) {
            String scope = dependency.getScope().getScope();
            NbDependencyType dependencyType;
            if ("COMPILE".equalsIgnoreCase(scope) || "PROVIDED".equalsIgnoreCase(scope)) {
                dependencyType = NbDependencyType.COMPILE;
            }
            else if ("TEST".equalsIgnoreCase(scope)) {
                dependencyType = NbDependencyType.TEST_COMPILE;
            }
            else if ("RUNTIME".equalsIgnoreCase(scope)) {
                dependencyType = NbDependencyType.RUNTIME;
            }
            else {
                dependencyType = NbDependencyType.OTHER;
            }

            if (dependency instanceof IdeaModuleDependency) {
                IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;

                NbGradleModule parsedDependency = tryParseModule(moduleDep.getDependencyModule(), parsedModules);
                if (parsedDependency != null) {
                    dependencies.addModuleDependency(
                            dependencyType,
                            new NbModuleDependency(parsedDependency, true));
                }
            }
            else if (dependency instanceof ExternalDependency) {
                ExternalDependency externalDep = (ExternalDependency)dependency;
                URI uri = Utilities.toURI(externalDep.getFile());

                File src = externalDep.getSource();
                URI srcUri = src != null
                        ? Utilities.toURI(src)
                        : null;

                dependencies.addUriDependency(
                        dependencyType,
                        new NbUriDependency(uri, srcUri, true));
            }
            else {
                LOGGER.log(Level.WARNING, "Unknown dependency: {0}", dependency);
            }
        }
        Map<NbDependencyType, NbDependencyGroup> dependencyMap
                = new EnumMap<NbDependencyType, NbDependencyGroup>(NbDependencyType.class);
        for (NbDependencyType type: NbDependencyType.values()) {
            NbDependencyGroup group = dependencies.getGroup(type);
            if (!group.isEmpty()) {
                dependencyMap.put(type, group);
            }
        }
        return dependencyMap;
    }

    private static boolean isResourcePath(IdeaSourceDirectory srcDir) {
        return srcDir.getDirectory().getName().toLowerCase(Locale.US).startsWith("resource");
    }

    private static Map<NbSourceType, NbSourceGroup> getSources(IdeaModule module) {
        List<File> sources = new LinkedList<File>();
        List<File> resources = new LinkedList<File>();
        List<File> testSources = new LinkedList<File>();
        List<File> testResources = new LinkedList<File>();

        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (IdeaSourceDirectory ideaSrcDir: contentRoot.getSourceDirectories()) {
                if (isResourcePath(ideaSrcDir)) {
                    resources.add(ideaSrcDir.getDirectory());
                }
                else {
                    sources.add(ideaSrcDir.getDirectory());
                }
            }
            for (IdeaSourceDirectory ideaTestDir: contentRoot.getTestDirectories()) {
                if (isResourcePath(ideaTestDir)) {
                    testResources.add(ideaTestDir.getDirectory());
                }
                else {
                    testSources.add(ideaTestDir.getDirectory());
                }
            }
        }

        Map<NbSourceType, NbSourceGroup> groups = new EnumMap<NbSourceType, NbSourceGroup>(NbSourceType.class);
        if (!sources.isEmpty()) {
            groups.put(NbSourceType.SOURCE,
                    new NbSourceGroup(NbModelUtils.nameSourceRoots(sources)));
        }
        if (!resources.isEmpty()) {
            groups.put(NbSourceType.RESOURCE,
                    new NbSourceGroup(NbModelUtils.nameSourceRoots(resources)));
        }
        if (!testSources.isEmpty()) {
            groups.put(NbSourceType.TEST_SOURCE,
                    new NbSourceGroup(NbModelUtils.nameSourceRoots(testSources)));
        }
        if (!testResources.isEmpty()) {
            groups.put(NbSourceType.TEST_RESOURCE,
                    new NbSourceGroup(NbModelUtils.nameSourceRoots(testResources)));
        }
        return groups;
    }

    private static List<IdeaModule> getChildModules(IdeaModule mainModule) {
        Collection<? extends GradleProject> children = mainModule.getGradleProject().getChildren();
        Set<String> childrenPaths = new HashSet<String>(2 * children.size());
        for (GradleProject child: children) {
            childrenPaths.add(child.getPath());
        }

        List<IdeaModule> result = new LinkedList<IdeaModule>();
        for (IdeaModule module: mainModule.getProject().getModules()) {
            if (childrenPaths.contains(module.getGradleProject().getPath())) {
                result.add(module);
            }
        }
        return result;
    }

    private static List<File> lookupListedDirs(Map<NbSourceType, NbSourceGroup> sources) {
        List<File> result = new LinkedList<File>();

        NbSourceGroup sourceGroups = sources.get(NbSourceType.SOURCE);
        if (sourceGroups != null) {
            for (NbSourceRoot sourceRoot: sourceGroups.getPaths()) {
                File parent = sourceRoot.getPath().getParentFile();
                if (parent != null) {
                    File webapp = new File(parent, "webapp");
                    if (webapp.isDirectory()) {
                        result.add(webapp);
                    }
                }
            }
        }

        return result;
    }

    private static NbGradleModule tryParseModule(IdeaModule module,
            Map<String, NbGradleModule> parsedModules) {
        String uniqueName = module.getGradleProject().getPath();

        NbGradleModule parsedModule = parsedModules.get(uniqueName);
        if (parsedModule != null) {
            return parsedModule;
        }

        Map<NbSourceType, NbSourceGroup> sources = getSources(module);

        File moduleDir = tryGetModuleDir(module);
        if (moduleDir == null) {
            LOGGER.log(Level.WARNING, "Unable to find the project directory: {0}", uniqueName);
            return null;
        }

        List<NbGradleTask> taskNames = new LinkedList<NbGradleTask>();
        for (GradleTask task: module.getGradleProject().getTasks()) {
            String qualifiedName = task.getPath();
            String description = task.getDescription();
            if (description == null) description = "";

            taskNames.add(new NbGradleTask(qualifiedName, description.trim()));
        }

        String defaultLevel = AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        String sourceLevel = module.getProject().getLanguageLevel().getLevel();
        sourceLevel = sourceLevel != null
                ? sourceLevel.replace("JDK_", "").replace("_", ".")
                : defaultLevel;

        String targetLevel = module.getProject().getJdkName();
        if (targetLevel == null) targetLevel = defaultLevel;

        sourceLevel = sourceLevel.trim();
        targetLevel = targetLevel.trim();

        String scriptDisplayName = module.getName();
        if (scriptDisplayName == null) scriptDisplayName = "";

        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                scriptDisplayName,
                uniqueName,
                moduleDir,
                createDefaultOutput(moduleDir),
                sourceLevel,
                targetLevel,
                taskNames);
        List<File> listedDirs = lookupListedDirs(sources);

        NbGradleModuleBuilder moduleBuilder = new NbGradleModuleBuilder(properties, sources, listedDirs);
        NbGradleModule result = moduleBuilder.getReadOnlyView();
        parsedModules.put(uniqueName, result);

        // Recursion is only allowed from this point to avoid infinite
        // recursion.

        moduleBuilder.addDependencies(getDependencies(module, parsedModules));

        for (IdeaModule child: getChildModules(module)) {
            NbGradleModule parsedChild = tryParseModule(child, parsedModules);
            if (parsedChild == null) {
                LOGGER.log(Level.WARNING, "Failed to parse a child module: {0}", child.getName());
            }
            else {
                moduleBuilder.addChild(parsedChild);
            }
        }

        return result;
    }

    private static void introduceLoadedModel(NbGradleModel model) {
        CACHE.addToCache(model);
        LISTENERS.fireEvent(model);
    }

    private static NbGradleModel parseFromIdeaModel(
            File projectDir, IdeaProject ideaModel, Lookup otherModels) throws IOException {
        IdeaModule mainModule = tryFindMainModule(projectDir, ideaModel);
        if (mainModule == null) {
            throw new IOException("Unable to find the main project in the model.");
        }

        Map<String, NbGradleModule> parsedModules = new HashMap<String, NbGradleModule>();
        NbGradleModule parsedMainModule = tryParseModule(mainModule, parsedModules);
        if (parsedMainModule == null) {
            throw new IOException("Unable to parse the main project from the model.");
        }

        for (IdeaModule module: ideaModel.getModules()) {
            String uniqueName = module.getGradleProject().getPath();

            NbGradleModule parsedModule = parsedModules.get(uniqueName);
            if (parsedModule == null) {
                tryParseModule(module, parsedModules);
            }
        }

        NbGradleModel mainModel = new NbGradleModel(projectDir, parsedMainModule, otherModels);
        File settings = mainModel.getSettingsFile();

        for (NbGradleModule module: parsedModules.values()) {
            if (module != null && module != parsedMainModule) {
                File moduleDir = module.getModuleDir();
                if (moduleDir != null) {
                    NbGradleModel model = new NbGradleModel(moduleDir, settings, module, otherModels);
                    introduceLoadedModel(model);
                }
            }
        }

        introduceLoadedModel(mainModel);

        return mainModel;
    }

    private static Lookup getExtensionModels(
            NbGradleProject project,
            ProgressHandle progress,
            ProjectConnection projectConnection) {

        List<Object> models = new LinkedList<Object>();
        for (GradleProjectExtension extension: project.getExtensions()) {
            for (List<Class<?>> modelRequest: extension.getGradleModels()) {
                for (Class<?> modelClass: modelRequest) {
                    try {
                        models.add(getRawModelWithProgress(project, progress, projectConnection, modelClass));
                        break;
                    } catch (UnknownModelException ex) {
                        LOGGER.log(Level.FINE, "Cannot find model {0}", modelClass.getName());
                    }
                }
            }
        }
        return Lookups.fixed(models.toArray());
    }

    private static NbGradleModel loadModelWithProgress(
            NbGradleProject project,
            ProgressHandle progress) throws IOException {
        File projectDir = project.getProjectDirectoryAsFile();

        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        IdeaProject ideaModel = null;
        Lookup extensionModels;

        //Lookup
        GradleConnector gradleConnector = createGradleConnector(project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            try {
                ideaModel = getModelWithProgress(project, progress, projectConnection, IdeaProject.class);
            } catch (UnknownModelException ex) {
                LOGGER.log(Level.INFO, "IdeaProject model is not found in project {0}", projectDir);
            }
            extensionModels = getExtensionModels(project, progress, projectConnection);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        if (ideaModel != null) {
            progress.progress(NbStrings.getParsingModel());
            return parseFromIdeaModel(projectDir, ideaModel, extensionModels);
        }
        else {
            return createEmptyModel(projectDir, extensionModels);
        }
    }

    private static class DependencyBuilder {
        private final Map<NbDependencyType, List<NbModuleDependency>> moduleDependencies;
        private final Map<NbDependencyType, List<NbUriDependency>> uriDependencies;

        public DependencyBuilder() {
            this.moduleDependencies = new EnumMap<NbDependencyType, List<NbModuleDependency>>(NbDependencyType.class);
            this.uriDependencies = new EnumMap<NbDependencyType, List<NbUriDependency>>(NbDependencyType.class);
        }

        public static <T> void addDependency(
                NbDependencyType type,
                T dependency,
                Map<NbDependencyType, List<T>> storage) {
            List<T> list = storage.get(type);
            if (list == null) {
                list = new LinkedList<T>();
                storage.put(type, list);
            }
            list.add(dependency);
        }

        public void addModuleDependency(NbDependencyType type, NbModuleDependency dependency) {
            addDependency(type, dependency, moduleDependencies);
        }

        public void addUriDependency(NbDependencyType type, NbUriDependency dependency) {
            addDependency(type, dependency, uriDependencies);
        }

        private static <T> List<T> getDependencies(
                NbDependencyType type,
                Map<NbDependencyType, List<T>> storage) {
            List<T> dependencies = storage.get(type);
            return dependencies != null ? dependencies : Collections.<T>emptyList();
        }

        public NbDependencyGroup getGroup(NbDependencyType type) {
            return new NbDependencyGroup(
                    getDependencies(type, moduleDependencies),
                    getDependencies(type, uriDependencies));
        }
    }

    private static final class DependenciesResult {
        private final boolean circular;
        private final Map<NbDependencyType, NbDependencyGroup> dependencies;

        public DependenciesResult(
                boolean circular,
                Map<NbDependencyType, NbDependencyGroup> dependencies) {
            this.circular = circular;
            this.dependencies = dependencies;
        }

        public boolean hasCircular() {
            return circular;
        }

        public Map<NbDependencyType, NbDependencyGroup> getDependencies() {
            return dependencies;
        }
    }

    private GradleModelLoader() {
        throw new AssertionError();
    }
}
