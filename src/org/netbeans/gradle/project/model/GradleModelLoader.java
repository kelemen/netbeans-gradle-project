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
import org.netbeans.api.progress.ProgressHandle;
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

    public static void addModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.addListener(listener);
    }

    public static void removeModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.removeListener(listener);
    }

    public static GradleConnector createGradleConnector() {
        GradleConnector result = GradleConnector.newConnector();
        FileObject gradleHomeObj = GlobalGradleSettings.getGradleHome().getValue();
        File gradleHome = gradleHomeObj != null ? FileUtil.toFile(gradleHomeObj) : null;

        if (gradleHome != null) {
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
            final FileObject projectDir,
            final ModelRetrievedListener listener) {
        fetchModel(projectDir, false, listener);
    }

    public static void fetchModel(
            final FileObject projectDir,
            final boolean mayFetchFromCache,
            final ModelRetrievedListener listener) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (listener == null) throw new NullPointerException("listener");

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
                        model = loadModelWithProgress(projectDir, progress);
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

    private static <T extends Model> T getModelWithProgress(
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<T> model) {
        ModelBuilder<T> builder = projectConnection.model(model);

        FileObject jdkHomeObj = GlobalGradleSettings.getCurrentGradleJdkHome();
        File jdkHome = jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
        if (jdkHome != null) {
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
            if ("COMPILE".equals(scope)) {
                dependencyType = NbDependencyType.COMPILE;
            }
            else if ("TEST".equals(scope)) {
                dependencyType = NbDependencyType.TEST_COMPILE;
            }
            else if ("RUNTIME".equals(scope)) {
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

        if (parsedModules.containsKey(uniqueName)) {
            NbGradleModule parsedModule = parsedModules.get(uniqueName);
            if (parsedModule == null) {
                LOGGER.log(Level.WARNING, "Circular or missing dependency: {0}", uniqueName);
            }
            return parsedModule;
        }
        parsedModules.put(uniqueName, null);

        Map<NbDependencyType, NbDependencyGroup> dependencies
                = getDependencies(module, parsedModules);

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

        List<NbGradleModule> children = new LinkedList<NbGradleModule>();
        for (IdeaModule child: getChildModules(module)) {
            NbGradleModule parsedChild = tryParseModule(child, parsedModules);
            if (parsedChild == null) {
                LOGGER.log(Level.WARNING, "Failed to parse a child module: {0}", child.getName());
            }
            else {
                children.add(parsedChild);
            }
        }

        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                uniqueName,
                moduleDir,
                createDefaultOutput(moduleDir),
                taskNames);

        List<File> listedDirs = lookupListedDirs(sources);
        NbGradleModule result = new NbGradleModule(properties, sources, listedDirs, dependencies, children);
        parsedModules.put(uniqueName, result);
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
            if (!parsedModules.containsKey(uniqueName)) {
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
            FileObject projectDir,
            ProgressHandle progress) throws IOException {
        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        IdeaProject ideaModel;

        GradleConnector gradleConnector = createGradleConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            ideaModel = getModelWithProgress(progress, projectConnection, IdeaProject.class);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

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

    private GradleModelLoader() {
        throw new AssertionError();
    }
}
