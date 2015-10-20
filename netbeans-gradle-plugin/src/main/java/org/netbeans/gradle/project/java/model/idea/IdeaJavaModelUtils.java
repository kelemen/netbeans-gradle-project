package org.netbeans.gradle.project.java.model.idea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.model.JavaModelSource;
import org.netbeans.gradle.project.java.model.JavaProjectDependency;
import org.netbeans.gradle.project.java.model.JavaProjectReference;
import org.netbeans.gradle.project.java.model.NbCodeCoverage;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.properties.standard.SourceLevelProperty;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

public final class IdeaJavaModelUtils {
    private static final Logger LOGGER = Logger.getLogger(IdeaJavaModelUtils.class.getName());

    public static NbJavaModel createEmptyModel(FileObject projectDir) {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IllegalStateException("Project directory does not exist.");
        }
        return createEmptyModel(projectDirAsFile, Lookup.EMPTY);
    }

    public static NbJavaModel createEmptyModel(File projectDir) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");

        return createEmptyModel(projectDir, Lookup.EMPTY);
    }

    private static File getDefaultBuildDir(File projectDir) {
        return new File(projectDir, "build");
    }


    private static File getDefaultMainClasses(File projectDir) {
        File classesDir = new File(getDefaultBuildDir(projectDir), "classes");
        return new File(classesDir, "main");
    }

    private static File getDefaultMainClasses(IdeaModule module) {
        File moduleDir = tryGetModuleDir(module);
        return moduleDir != null ? getDefaultMainClasses(moduleDir) : null;
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

    private static NbJavaModel createUnreliableModel(
            NbJavaModule mainModule,
            Map<? extends File, ? extends JavaProjectDependency> possibleDependencies) {
        return NbJavaModel.createModel(JavaModelSource.COMPATIBLE_API, mainModule, possibleDependencies);
    }

    public static NbJavaModel createEmptyModel(File projectDir, Lookup otherModels) {
        String name = projectDir.getName();
        String level = SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        GenericProjectProperties properties = new GenericProjectProperties(name, name, projectDir);
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel(level, level);

        NbJavaModule result = new NbJavaModule(
                properties,
                compatibilityModel,
                Collections.<JavaSourceSet>emptyList(),
                Collections.<NbListedDir>emptyList(),
                JavaTestModel.getDefaulTestModel(projectDir),
                NbCodeCoverage.NO_CODE_COVERAGE
        );

        return createUnreliableModel(result,
                Collections.<File, JavaProjectDependency>emptyMap());
    }

    private static Collection<JavaSourceGroup> fromIdeaSourceRoots(Collection<? extends IdeaSourceDirectory> roots) {
        Map<JavaSourceGroupName, List<File>> sourceRootMap
                = new EnumMap<>(JavaSourceGroupName.class);

        for (IdeaSourceDirectory root: roots) {
            File dir = root.getDirectory();
            String name = dir.getName().toLowerCase(Locale.US);

            JavaSourceGroupName choice;
            if (name.startsWith("resource")) {
                choice = JavaSourceGroupName.RESOURCES;
            }
            else if ("groovy".equals(name)) {
                choice = JavaSourceGroupName.GROOVY;
            }
            else if ("scala".equals(name)) {
                choice = JavaSourceGroupName.SCALA;
            }
            else {
                choice = JavaSourceGroupName.JAVA;
            }

            List<File> rootsList = sourceRootMap.get(choice);
            if (rootsList == null) {
                rootsList = new LinkedList<>();
                sourceRootMap.put(choice, rootsList);
            }
            rootsList.add(dir);
        }

        List<JavaSourceGroup> result = new ArrayList<>(sourceRootMap.size());
        for (Map.Entry<JavaSourceGroupName, List<File>> entry: sourceRootMap.entrySet()) {
            result.add(new JavaSourceGroup(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static List<JavaSourceSet> parseSourceSets(
            IdeaModule module,
            File projectDir,
            Map<String, IdeaDependencyBuilder> cache) {

        ProjectClassPaths classPaths = fetchAllDependencies(module, cache);
        File buildOutputDir = getDefaultBuildDir(projectDir);
        File classesDir = new File(buildOutputDir, "classes");
        File resourcesDir = new File(buildOutputDir, "resources");

        JavaOutputDirs mainOutputs = new JavaOutputDirs(
                new File(classesDir, JavaSourceSet.NAME_MAIN),
                new File(resourcesDir, JavaSourceSet.NAME_MAIN),
                Collections.<File>emptyList());

        JavaSourceSet.Builder main
                = new JavaSourceSet.Builder(JavaSourceSet.NAME_MAIN, mainOutputs);
        main.setClasspaths(classPaths.main);

        JavaOutputDirs testOutputs = new JavaOutputDirs(
                new File(classesDir, JavaSourceSet.NAME_TEST),
                new File(resourcesDir, JavaSourceSet.NAME_TEST),
                Collections.<File>emptyList());

        JavaSourceSet.Builder test
                = new JavaSourceSet.Builder(JavaSourceSet.NAME_TEST, testOutputs);
        test.setClasspaths(classPaths.test);

        int mainSourceRootCount = 0;
        int testSourceRootCount = 0;

        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (JavaSourceGroup group: fromIdeaSourceRoots(contentRoot.getSourceDirectories())) {
                main.addSourceGroup(group);
                mainSourceRootCount += group.getSourceRoots().size();
            }
            for (JavaSourceGroup group: fromIdeaSourceRoots(contentRoot.getTestDirectories())) {
                test.addSourceGroup(group);
                testSourceRootCount += group.getSourceRoots().size();
            }
        }

        List<JavaSourceSet> result = new ArrayList<>(2);
        if (mainSourceRootCount > 0) {
            result.add(main.create());
        }
        if (testSourceRootCount > 0) {
            result.add(test.create());
        }
        return result;
    }

    private static ProjectClassPaths fetchAllDependencies(
            IdeaModule module,
            Map<String, IdeaDependencyBuilder> cache) {

        IdeaDependencyBuilder result = new IdeaDependencyBuilder();
        fetchAllDependencies(module, result, Collections.<String>emptySet(), cache);

        JavaClassPaths mainClassPath = new JavaClassPaths(result.mainCompile, result.mainRuntime);
        JavaClassPaths testClassPath = new JavaClassPaths(result.testCompile, result.testRuntime);
        return new ProjectClassPaths(mainClassPath, testClassPath);
    }

    private static void fetchAllDependencies(
            IdeaModule module,
            IdeaDependencyBuilder result,
            Set<String> projectsToSkip,
            Map<String, IdeaDependencyBuilder> cache) {

        String uniqueProjectName = module.getGradleProject().getPath();

        if (projectsToSkip.contains(uniqueProjectName)) {
            return;
        }

        IdeaDependencyBuilder parsed = cache.get(uniqueProjectName);
        if (parsed != null) {
            result.setFrom(parsed);
            return;
        }

        File defaultMainBuildDir = getDefaultMainClasses(module);
        if (defaultMainBuildDir != null) {
            result.addTestCompile(defaultMainBuildDir);
        }

        Set<String> nextProjectsToSkip = null;

        for (IdeaDependency dependency: module.getDependencies()) {
            String scope = dependency.getScope().getScope();
            IdeaDependencyType dependencyType = IdeaDependencyType.fromIdeaScope(scope);

            if (dependency instanceof IdeaModuleDependency) {
                if (nextProjectsToSkip == null) {
                    nextProjectsToSkip = new HashSet<>(projectsToSkip);
                    nextProjectsToSkip.add(uniqueProjectName);
                }

                IdeaModule moduleDep = ((IdeaModuleDependency)dependency).getDependencyModule();

                IdeaDependencyBuilder subDependencies = new IdeaDependencyBuilder();
                fetchAllDependencies(moduleDep, subDependencies, nextProjectsToSkip, cache);

                File mainBuildDir = getDefaultMainClasses(moduleDep);
                if (mainBuildDir != null) {
                    result.add(dependencyType, mainBuildDir);
                }
                result.addAll(dependencyType, subDependencies);
            }
            else if (dependency instanceof ExternalDependency) {
                ExternalDependency externalDep = (ExternalDependency)dependency;
                result.add(dependencyType, externalDep.getFile());
            }
            else {
                LOGGER.log(Level.WARNING, "Unknown dependency: {0}", dependency);
            }
        }

        cache.put(uniqueProjectName, result);
    }

    private static NbListedDir findWebAppDir(Collection<JavaSourceSet> sources) {
        for (JavaSourceSet sourceSet: sources) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File sourceRoot: sourceGroup.getSourceRoots()) {
                    File parent = sourceRoot.getParentFile();
                    if (parent != null) {
                        File webapp = new File(parent, "webapp");
                        if (webapp.isDirectory()) {
                            return new NbListedDir(NbStrings.getWebPages(), webapp);
                        }
                    }
                }
            }
        }

        return null;
    }

    private static List<NbListedDir> lookupListedDirs(Collection<JavaSourceSet> sources) {
        List<NbListedDir> result = new LinkedList<>();

        NbListedDir webAppDir = findWebAppDir(sources);
        if (webAppDir != null) {
            result.add(webAppDir);
        }

        return result;
    }

    private static NbJavaModule tryParseModule(IdeaModule module,
            Map<String, IdeaDependencyBuilder> cache) {
        String uniqueName = module.getGradleProject().getPath();

        File moduleDir = tryGetModuleDir(module);
        if (moduleDir == null) {
            LOGGER.log(Level.WARNING, "Unable to find the project directory: {0}", uniqueName);
            return null;
        }

        List<JavaSourceSet> sourceSets = parseSourceSets(module, moduleDir, cache);

        String defaultLevel = SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault());

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

        GenericProjectProperties properties = new GenericProjectProperties(scriptDisplayName, uniqueName, moduleDir);
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel(sourceLevel, targetLevel);

        List<NbListedDir> listedDirs = lookupListedDirs(sourceSets);
        JavaTestModel testModel = JavaTestModel.getDefaulTestModel(moduleDir);

        return new NbJavaModule(properties, compatibilityModel, sourceSets, listedDirs, testModel, NbCodeCoverage.NO_CODE_COVERAGE);
    }

    public static Map<File, NbJavaModel> parseFromIdeaModel(File projectDir, IdeaProject ideaModel) throws IOException {
        IdeaModule mainModule = tryFindMainModule(projectDir, ideaModel);
        if (mainModule == null) {
            throw new IOException("Unable to find the main project in the model.");
        }

        DomainObjectSet<? extends IdeaModule> modules = ideaModel.getModules();
        int modulesCount = modules.size();

        Map<String, IdeaDependencyBuilder> cache = CollectionUtils.newHashMap(modulesCount);

        Map<File, NbJavaModule> parsedModules = CollectionUtils.newHashMap(modulesCount);
        for (IdeaModule module: modules) {
            NbJavaModule parsedModule = tryParseModule(module, cache);
            if (parsedModule != null) {
                parsedModules.put(parsedModule.getModuleDir(), parsedModule);
            }
        }

        if (!parsedModules.containsKey(projectDir)) {
            throw new IOException("Unable to parse the main project from the model.");
        }

        Map<File, JavaProjectReference> asDependency = CollectionUtils.newHashMap(modulesCount);
        Map<File, JavaProjectDependency> outputDirToProject = CollectionUtils.newHashMap(modulesCount);

        for (NbJavaModule module: parsedModules.values()) {
            File moduleDir = module.getModuleDir();
            JavaProjectReference projectRef = new JavaProjectReference(moduleDir, module);
            asDependency.put(moduleDir, projectRef);

            for (JavaSourceSet sourceSet: module.getSources()) {
                File classesDir = sourceSet.getOutputDirs().getClassesDir();
                String sourceSetName = sourceSet.getName();

                outputDirToProject.put(classesDir, new JavaProjectDependency(sourceSetName, projectRef));
            }
        }

        Map<File, NbJavaModel> result = CollectionUtils.newHashMap(modulesCount);
        for (NbJavaModule module: parsedModules.values()) {
            if (module.getSources().isEmpty()) {
                LOGGER.log(Level.INFO,
                        "Disabling the Java extension because there are no sources: {0}",
                        module.getProperties().getProjectDir());
            }
            else {
                NbJavaModel model = createUnreliableModel(module, outputDirToProject);
                result.put(module.getModuleDir(), model);
            }
        }
        return result;
    }

    private static class ProjectClassPaths {
        public final JavaClassPaths main;
        public final JavaClassPaths test;

        public ProjectClassPaths(JavaClassPaths main, JavaClassPaths test) {
            assert main != null;
            assert test != null;

            this.main = main;
            this.test = test;
        }
    }

    private static class IdeaDependencyBuilder {
        private Set<File> mainCompile;
        private Set<File> mainRuntime;
        private Set<File> testCompile;
        private Set<File> testRuntime;

        public IdeaDependencyBuilder() {
            this.mainCompile = new LinkedHashSet<>();
            this.mainRuntime = new LinkedHashSet<>();
            this.testCompile = new LinkedHashSet<>();
            this.testRuntime = new LinkedHashSet<>();
        }

        public void addAll(IdeaDependencyType type, IdeaDependencyBuilder dependencies) {
            switch (type) {
                case COMPILE:
                    addMainCompile(dependencies);
                    break;
                case PROVIDED_COMPILE:
                    addMainProvidedCompile(dependencies);
                    break;
                case RUNTIME:
                    addMainRuntime(dependencies);
                    break;
                case TEST_COMPILE:
                    addTestCompile(dependencies);
                    break;
                case TEST_RUNTIME:
                    addTestRuntime(dependencies);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unknown dependency type: {0}", type);
                    break;
            }
        }

        public void addMainCompile(IdeaDependencyBuilder dependencies) {
            addMainCompile(dependencies.mainCompile);
            addMainRuntime(dependencies.mainRuntime);
        }

        public void addMainProvidedCompile(IdeaDependencyBuilder dependencies) {
            addMainProvidedCompile(dependencies.mainCompile);
            addMainRuntime(dependencies.mainRuntime);
        }

        public void addMainRuntime(IdeaDependencyBuilder dependencies) {
            addMainRuntime(dependencies.mainRuntime);
        }

        public void addTestCompile(IdeaDependencyBuilder dependencies) {
            addTestCompile(dependencies.mainCompile);
            addTestRuntime(dependencies.mainRuntime);
        }

        public void addTestRuntime(IdeaDependencyBuilder dependencies) {
            addTestRuntime(dependencies.mainRuntime);
        }

        public void addAll(IdeaDependencyType type, Collection<File> files) {
            switch (type) {
                case COMPILE:
                    addMainCompile(files);
                    break;
                case PROVIDED_COMPILE:
                    addMainProvidedCompile(files);
                    break;
                case RUNTIME:
                    addMainRuntime(files);
                    break;
                case TEST_COMPILE:
                    addTestCompile(files);
                    break;
                case TEST_RUNTIME:
                    addTestRuntime(files);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unknown dependency type: {0}", type);
                    break;
            }
        }

        public void addMainCompile(Collection<File> files) {
            mainCompile.addAll(files);
            mainRuntime.addAll(files);
            testCompile.addAll(files);
            testRuntime.addAll(files);
        }

        public void addMainProvidedCompile(Collection<File> files) {
            mainCompile.addAll(files);
            testCompile.addAll(files);
        }

        public void addMainRuntime(Collection<File> files) {
            mainRuntime.addAll(files);
            testRuntime.addAll(files);
        }

        public void addTestCompile(Collection<File> files) {
            testCompile.addAll(files);
            testRuntime.addAll(files);
        }

        public void addTestRuntime(Collection<File> files) {
            testRuntime.addAll(files);
        }

        public void add(IdeaDependencyType type, File file) {
            switch (type) {
                case COMPILE:
                    addMainCompile(file);
                    break;
                case PROVIDED_COMPILE:
                    addMainProvidedCompile(file);
                    break;
                case RUNTIME:
                    addMainRuntime(file);
                    break;
                case TEST_COMPILE:
                    addTestCompile(file);
                    break;
                case TEST_RUNTIME:
                    addTestRuntime(file);
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unknown dependency type: {0}", type);
                    break;
            }
        }

        public void addMainCompile(File file) {
            mainCompile.add(file);
            mainRuntime.add(file);
            testCompile.add(file);
            testRuntime.add(file);
        }

        public void addMainProvidedCompile(File file) {
            mainCompile.add(file);
            testCompile.add(file);
        }

        public void addMainRuntime(File file) {
            mainRuntime.add(file);
            testRuntime.add(file);
        }

        public void addTestCompile(File file) {
            testCompile.add(file);
            testRuntime.add(file);
        }

        public void addTestRuntime(File file) {
            testRuntime.add(file);
        }

        public void setFrom(IdeaDependencyBuilder other) {
            this.mainCompile = other.mainCompile;
            this.mainRuntime = other.mainRuntime;
            this.testCompile = other.testCompile;
            this.testRuntime = other.testRuntime;
        }
    }

    private IdeaJavaModelUtils() {
        throw new AssertionError();
    }
}
