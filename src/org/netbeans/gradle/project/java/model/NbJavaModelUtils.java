package org.netbeans.gradle.project.java.model;

import org.netbeans.gradle.project.model.NbGradleTask;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery;
import org.netbeans.gradle.project.model.EmptyGradleProject;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.model.GradleProjectInfo;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

public final class NbJavaModelUtils {
    private static final Logger LOGGER = Logger.getLogger(NbJavaModelUtils.class.getName());

    public static NbJavaModel createEmptyModel(FileObject projectDir) {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IllegalStateException("Project directory does not exist.");
        }
        return createEmptyModel(projectDirAsFile, Lookup.EMPTY);
    }

    public static NbJavaModel createEmptyModel(File projectDir) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        return createEmptyModel(projectDir, Lookup.EMPTY);
    }

    public static NbOutput createDefaultOutput(File projectDir) {
        File buildDir = new File(projectDir, "build" + File.separatorChar + "classes");

        return new NbOutput(
                new File(buildDir, "main"),
                new File(buildDir, "test"));
    }

    public static NbJavaModel createEmptyModel(File projectDir, Lookup otherModels) {
        String name = projectDir.getName();

        String level = AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        NbJavaModule.Properties properties = new NbJavaModule.Properties(
                name,
                name,
                projectDir,
                createDefaultOutput(projectDir),
                level,
                level,
                Collections.<NbGradleTask>emptyList());

        NbJavaModuleBuilder mainModuleBuilder = new NbJavaModuleBuilder(
                new EmptyGradleProject(projectDir),
                properties,
                Collections.<NbSourceType, NbSourceGroup>emptyMap(),
                Collections.<File>emptyList());

        return new NbJavaModel(mainModuleBuilder.getReadOnlyView());
    }

    private static void getAllChildren(GradleProjectInfo module, List<GradleProjectInfo> result) {
        Collection<GradleProjectInfo> children = module.getChildren();
        result.addAll(children);
        for (GradleProjectInfo child: children) {
            getAllChildren(child, result);
        }
    }

    public static List<GradleProjectInfo> getAllChildren(GradleProjectInfo module) {
        List<GradleProjectInfo> result = new LinkedList<GradleProjectInfo>();
        getAllChildren(module, result);
        return result;
    }

    public static List<GradleProjectInfo> getAllChildren(NbGradleModel model) {
        List<GradleProjectInfo> result = new LinkedList<GradleProjectInfo>();
        getAllChildren(model.getGradleProjectInfo(), result);
        return result;
    }

    private static void getAllDependencies(
            NbJavaModule module,
            NbDependencyType type,
            Collection<NbJavaDependency> toAdd,
            Set<String> toSkip) {
        if (!toSkip.add(module.getUniqueName())) {
            return;
        }

        for (NbJavaDependency dependency: module.getDependencies(NbDependencyType.COMPILE).getAllDependencies()) {
            toAdd.add(dependency);
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                NbDependencyType recType;
                switch (type) {
                    case RUNTIME:
                        recType = NbDependencyType.RUNTIME;
                        break;
                    case TEST_RUNTIME:
                        recType = NbDependencyType.RUNTIME;
                        break;
                    default:
                        recType = NbDependencyType.COMPILE;
                        break;
                }
                getAllDependencies(moduleDep.getModule(), recType, toAdd, toSkip);
            }
        }

        if (type == NbDependencyType.RUNTIME || type == NbDependencyType.TEST_RUNTIME) {
            for (NbJavaDependency dependency: module.getDependencies(NbDependencyType.RUNTIME).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    getAllDependencies(moduleDep.getModule(), NbDependencyType.RUNTIME, toAdd, toSkip);
                }
            }
        }

        if (type == NbDependencyType.TEST_COMPILE || type == NbDependencyType.TEST_RUNTIME) {
            for (NbJavaDependency dependency: module.getDependencies(NbDependencyType.TEST_COMPILE).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    NbDependencyType recType;
                    switch (type) {
                        case TEST_COMPILE:
                            recType = NbDependencyType.COMPILE;
                            break;
                        case TEST_RUNTIME:
                            recType = NbDependencyType.RUNTIME;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    getAllDependencies(moduleDep.getModule(), recType, toAdd, toSkip);
                }
            }
        }

        if (type == NbDependencyType.TEST_RUNTIME) {
            for (NbJavaDependency dependency: module.getDependencies(NbDependencyType.TEST_RUNTIME).getAllDependencies()) {
                toAdd.add(dependency);
                if (dependency instanceof NbModuleDependency) {
                    NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                    getAllDependencies(moduleDep.getModule(), NbDependencyType.RUNTIME, toAdd, toSkip);
                }
            }
        }
    }

    public static Collection<NbJavaDependency> getAllDependencies(
            NbJavaModule module, NbDependencyType type) {
        if (module == null) throw new NullPointerException("module");
        if (type == null) throw new NullPointerException("type");
        if (type == NbDependencyType.OTHER) {
            throw new IllegalArgumentException("Cannot fetch this kind of dependencies: " + type);
        }

        Set<NbJavaDependency> dependencies = new LinkedHashSet<NbJavaDependency>();
        getAllDependencies(module, type, dependencies, new HashSet<String>());

        return dependencies;
    }

    public static Collection<NbJavaDependency> getAllDependencies(NbJavaModule module) {
        return getAllDependencies(module, NbDependencyType.TEST_RUNTIME);
    }

    public static Collection<NbJavaModule> getAllModuleDependencies(
            NbJavaModule module, NbDependencyType type) {
        List<NbJavaModule> result = new LinkedList<NbJavaModule>();
        for (NbJavaDependency dependency: getAllDependencies(module, type)) {
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                result.add(moduleDep.getModule());
            }
        }
        return result;
    }

    public static Collection<NbJavaModule> getAllModuleDependencies(NbJavaModule module) {
        return getAllModuleDependencies(module, NbDependencyType.TEST_RUNTIME);
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

    private static Map<NbDependencyType, NbDependencyGroup> getDependencies(
            IdeaModule module, Map<String, NbJavaModule> parsedModules) {

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

                NbJavaModule parsedDependency = tryParseModule(moduleDep.getDependencyModule(), parsedModules);
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
                    new NbSourceGroup(GradleModelLoader.nameSourceRoots(sources)));
        }
        if (!resources.isEmpty()) {
            groups.put(NbSourceType.RESOURCE,
                    new NbSourceGroup(GradleModelLoader.nameSourceRoots(resources)));
        }
        if (!testSources.isEmpty()) {
            groups.put(NbSourceType.TEST_SOURCE,
                    new NbSourceGroup(GradleModelLoader.nameSourceRoots(testSources)));
        }
        if (!testResources.isEmpty()) {
            groups.put(NbSourceType.TEST_RESOURCE,
                    new NbSourceGroup(GradleModelLoader.nameSourceRoots(testResources)));
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

    private static NbJavaModule tryParseModule(IdeaModule module,
            Map<String, NbJavaModule> parsedModules) {
        String uniqueName = module.getGradleProject().getPath();

        NbJavaModule parsedModule = parsedModules.get(uniqueName);
        if (parsedModule != null) {
            return parsedModule;
        }

        Map<NbSourceType, NbSourceGroup> sources = getSources(module);

        File moduleDir = GradleModelLoader.tryGetModuleDir(module);
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

        NbJavaModule.Properties properties = new NbJavaModule.Properties(
                scriptDisplayName,
                uniqueName,
                moduleDir,
                NbJavaModelUtils.createDefaultOutput(moduleDir),
                sourceLevel,
                targetLevel,
                taskNames);
        List<File> listedDirs = lookupListedDirs(sources);

        NbJavaModuleBuilder moduleBuilder = new NbJavaModuleBuilder(
                module.getGradleProject(), properties, sources, listedDirs);
        NbJavaModule result = moduleBuilder.getReadOnlyView();
        parsedModules.put(uniqueName, result);

        // Recursion is only allowed from this point to avoid infinite
        // recursion.

        moduleBuilder.addDependencies(getDependencies(module, parsedModules));

        for (IdeaModule child: getChildModules(module)) {
            NbJavaModule parsedChild = tryParseModule(child, parsedModules);
            if (parsedChild == null) {
                LOGGER.log(Level.WARNING, "Failed to parse a child module: {0}", child.getName());
            }
            else {
                moduleBuilder.addChild(parsedChild);
            }
        }

        return result;
    }

    private static GradleProjectInfo createProjectInfo(NbJavaModule javaModule) {
        List<NbJavaModule> javaChildren = javaModule.getChildren();
        List<GradleProjectInfo> children = new ArrayList<GradleProjectInfo>(javaChildren.size());
        for (NbJavaModule child: javaChildren) {
            children.add(createProjectInfo(child));
        }

        return new GradleProjectInfo(
                javaModule.getGradleProject(),
                javaModule.getModuleDir(),
                children);
    }

    public static NbJavaModel parseFromIdeaModel(File projectDir, IdeaProject ideaModel) throws IOException {
        IdeaModule mainModule = GradleModelLoader.tryFindMainModule(projectDir, ideaModel);
        if (mainModule == null) {
            throw new IOException("Unable to find the main project in the model.");
        }

        Map<String, NbJavaModule> parsedModules = new HashMap<String, NbJavaModule>();
        NbJavaModule parsedMainModule = tryParseModule(mainModule, parsedModules);
        if (parsedMainModule == null) {
            throw new IOException("Unable to parse the main project from the model.");
        }

        for (IdeaModule module: ideaModel.getModules()) {
            String uniqueName = module.getGradleProject().getPath();

            NbJavaModule parsedModule = parsedModules.get(uniqueName);
            if (parsedModule == null) {
                tryParseModule(module, parsedModules);
            }
        }

        NbJavaModel mainModel = new NbJavaModel(parsedMainModule);

        // If only the JavaExtension is available then we know what model
        // would be created for projects in "parsedModules".
        //
        // This is just a hack to avoid regression when someone does not use
        // any extension.
        Collection<? extends GradleProjectExtensionQuery> extensions
                = Lookup.getDefault().lookupAll(GradleProjectExtensionQuery.class);

        if (extensions.size() == 1) {
            for (NbJavaModule module: parsedModules.values()) {
                if (module != null && module != parsedMainModule) {
                    File moduleDir = module.getModuleDir();
                    if (moduleDir != null) {
                        NbJavaModel javaModel = new NbJavaModel(module);
                        GradleProjectInfo projectInfo = createProjectInfo(module);
                        NbGradleModel model
                                = new NbGradleModel(projectInfo, module.getModuleDir(), Lookups.fixed(javaModel));
                       GradleModelLoader.introduceLoadedModel(model);
                    }
                }
            }
        }

        return mainModel;
    }

    public static File uriToFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            return Utilities.toFile(uri);
        }
        else {
            return null;
        }
    }

    public static FileObject uriToFileObject(URI uri) {
        File file = uriToFile(uri);
        return file != null ? FileUtil.toFileObject(file) : null;
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

    private NbJavaModelUtils() {
        throw new AssertionError();
    }
}
