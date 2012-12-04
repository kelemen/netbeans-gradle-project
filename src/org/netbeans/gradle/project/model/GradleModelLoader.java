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
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.tasks.DaemonTask;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

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

    public static GradleConnector createGradleConnector(NbGradleProject project) {
        GradleConnector result = GradleConnector.newConnector();
        File gradleHome = project.getProperties().getGradleHome().getValue();

        if (gradleHome != null && !gradleHome.getPath().isEmpty()) {
            result.useInstallation(gradleHome);
        }

        return result;
    }

    private static NbGradleModel tryGetFromCache(FileObject projectDir) {
        File projectDirFile = FileUtil.toFile(projectDir);
        FileObject settingFileObj = NbGradleModel.findSettingsGradle(projectDir);
        File settingsFile = settingFileObj != null
                ? FileUtil.toFile(settingFileObj)
                : null;
        if (settingsFile == null && settingFileObj != null) {
            LOGGER.log(Level.WARNING, "Settings file of the project disappeared: {0}", settingFileObj);
            return null;
        }

        return projectDirFile != null
                ? CACHE.tryGet(projectDirFile, settingsFile)
                : null;
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

        final FileObject projectDir = project.getProjectDirectory();
        String caption = NbStrings.getLoadingProjectText(projectDir.getNameExt());
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
        }, true);
    }

    private static NbOutput createDefaultOutput(File projectDir) {
        File buildDir = new File(projectDir, "build" + File.separatorChar + "classes");

        return new NbOutput(
                new File(buildDir, "main"),
                new File(buildDir, "test"));
    }

    public static NbGradleModel createEmptyModel(FileObject projectDir) throws IOException {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        String name = projectDir.getNameExt();
        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                name,
                projectDirAsFile,
                createDefaultOutput(projectDirAsFile),
                Collections.<NbGradleTask>emptyList());

        NbGradleModule mainModule = new NbGradleModule(properties,
                Collections.<NbSourceType, NbSourceGroup>emptyMap(),
                Collections.<File>emptyList(),
                Collections.<NbDependencyType, NbDependencyGroup>emptyMap(),
                Collections.<NbGradleModule>emptyList());

        return new NbGradleModel(projectDir, mainModule);
    }

    public static File getScriptJavaHome(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        JavaPlatform platform = project.getProperties().getScriptPlatform().getValue();
        FileObject jdkHomeObj = platform != null
                ? GlobalGradleSettings.getHomeFolder(platform)
                : null;
        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
    }

    private static <T extends Model> T getModelWithProgress(
            NbGradleProject project,
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<T> model) {
        ModelBuilder<T> builder = projectConnection.model(model);

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

    private static IdeaModule tryFindMainModule(FileObject projectDir, IdeaProject ideaModel) {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        for (IdeaModule module: ideaModel.getModules()) {
            File moduleDir = tryGetModuleDir(module);
            if (moduleDir != null && moduleDir.equals(projectDirAsFile)) {
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
            groups.put(NbSourceType.SOURCE, new NbSourceGroup(sources));
        }
        if (!resources.isEmpty()) {
            groups.put(NbSourceType.RESOURCE, new NbSourceGroup(resources));
        }
        if (!testSources.isEmpty()) {
            groups.put(NbSourceType.TEST_SOURCE, new NbSourceGroup(testSources));
        }
        if (!testResources.isEmpty()) {
            groups.put(NbSourceType.TEST_RESOURCE, new NbSourceGroup(testResources));
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
            for (File sourceRoot: sourceGroups.getPaths()) {
                File parent = sourceRoot.getParentFile();
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

        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                uniqueName,
                moduleDir,
                createDefaultOutput(moduleDir),
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
            FileObject projectDir, IdeaProject ideaModel) throws IOException {
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

        NbGradleModel mainModel = new NbGradleModel(projectDir, parsedMainModule);
        FileObject settings = mainModel.getSettingsFile();

        for (NbGradleModule module: parsedModules.values()) {
            if (module != null && module != parsedMainModule) {
                FileObject moduleDir = FileUtil.toFileObject(module.getModuleDir());
                if (moduleDir != null) {
                    NbGradleModel model = new NbGradleModel(moduleDir, settings, module);
                    introduceLoadedModel(model);
                }
            }
        }

        introduceLoadedModel(mainModel);

        return mainModel;
    }

    private static NbGradleModel loadModelWithProgress(
            NbGradleProject project,
            ProgressHandle progress) throws IOException {
        FileObject projectDir = project.getProjectDirectory();

        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        IdeaProject ideaModel;

        GradleConnector gradleConnector = createGradleConnector(project);
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            ideaModel = getModelWithProgress(project, progress, projectConnection, IdeaProject.class);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        progress.progress(NbStrings.getParsingModel());
        return parseFromIdeaModel(projectDir, ideaModel);
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
