package org.netbeans.gradle.project.java.model.idea;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.model.GradleProjectInfo;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.AbstractProjectProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

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
        if (projectDir == null) throw new NullPointerException("projectDir");
        return createEmptyModel(projectDir, Lookup.EMPTY);
    }

    private static File getDefaultBuildDir(File projectDir) {
        return new File(projectDir, "build");
    }

    private static NbOutput createDefaultOutput(File projectDir) {
        File buildDir = new File(getDefaultBuildDir(projectDir), "classes");

        return new NbOutput(
                new File(buildDir, "main"),
                new File(buildDir, "test"));
    }

    private static NbOutput createDefaultOutput(IdeaModule module) {
        File moduleDir = GradleModelLoader.tryGetModuleDir(module);
        return moduleDir != null ? createDefaultOutput(moduleDir) : null;
    }

    public static NbJavaModel createEmptyModel(File projectDir, Lookup otherModels) {
        String name = projectDir.getName();
        String level = AbstractProjectProperties.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        GenericProjectProperties properties = new GenericProjectProperties(name, name, projectDir);
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel(level, level);

        return new NbJavaModel(new NbJavaModule(
                properties,
                compatibilityModel,
                Collections.<JavaSourceSet>emptyList(),
                Collections.<File>emptyList()));
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

    private static Collection<JavaSourceGroup> fromIdeaSourceRoots(Collection<? extends IdeaSourceDirectory> roots) {
        List<File> javaRoots = new LinkedList<File>();
        List<File> resourceRoots = new LinkedList<File>();

        for (IdeaSourceDirectory root: roots) {
            File dir = root.getDirectory();
            if (isResourcePath(dir)) {
                resourceRoots.add(dir);
            }
            else {
                javaRoots.add(dir);
            }
        }
        return Arrays.asList(
                new JavaSourceGroup(JavaSourceGroupName.JAVA, javaRoots),
                new JavaSourceGroup(JavaSourceGroupName.RESOURCES, resourceRoots));
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

        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (JavaSourceGroup group: fromIdeaSourceRoots(contentRoot.getSourceDirectories())) {
                main.addSourceGroup(group);
            }
            for (JavaSourceGroup group: fromIdeaSourceRoots(contentRoot.getTestDirectories())) {
                test.addSourceGroup(group);
            }
        }

        return Arrays.asList(main.create(), test.create());
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

        NbOutput defaultOutput = createDefaultOutput(module);
        if (defaultOutput != null) {
            result.addTestCompile(defaultOutput.getBuildDir());
        }

        Set<String> nextProjectsToSkip = null;

        for (IdeaDependency dependency: module.getDependencies()) {
            String scope = dependency.getScope().getScope();
            IdeaDependencyType dependencyType = IdeaDependencyType.fromIdeaScope(scope);

            if (dependency instanceof IdeaModuleDependency) {
                if (nextProjectsToSkip == null) {
                    nextProjectsToSkip = new HashSet<String>(projectsToSkip);
                    nextProjectsToSkip.add(uniqueProjectName);
                }

                IdeaModule moduleDep = ((IdeaModuleDependency)dependency).getDependencyModule();

                IdeaDependencyBuilder subDependencies = new IdeaDependencyBuilder();
                fetchAllDependencies(moduleDep, subDependencies, nextProjectsToSkip, cache);

                NbOutput moduleOutput = createDefaultOutput(moduleDep);
                if (moduleOutput != null) {
                    result.add(dependencyType, moduleOutput.getBuildDir());
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

    private static List<File> lookupListedDirs(Collection<JavaSourceSet> sources) {
        List<File> result = new LinkedList<File>();

        for (JavaSourceSet sourceSet: sources) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File sourceRoot: sourceGroup.getSourceRoots()) {
                    File parent = sourceRoot.getParentFile();
                    if (parent != null) {
                        File webapp = new File(parent, "webapp");
                        if (webapp.isDirectory()) {
                            result.add(webapp);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean isResourcePath(File dir) {
        return dir.getName().toLowerCase(Locale.US).startsWith("resource");
    }

    private static NbJavaModule tryParseModule(IdeaModule module,
            Map<String, IdeaDependencyBuilder> cache) {
        String uniqueName = module.getGradleProject().getPath();

        File moduleDir = GradleModelLoader.tryGetModuleDir(module);
        if (moduleDir == null) {
            LOGGER.log(Level.WARNING, "Unable to find the project directory: {0}", uniqueName);
            return null;
        }

        List<JavaSourceSet> sourceSets = parseSourceSets(module, moduleDir, cache);

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

        GenericProjectProperties properties = new GenericProjectProperties(scriptDisplayName, uniqueName, moduleDir);
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel(sourceLevel, targetLevel);
        NbOutput output = IdeaJavaModelUtils.createDefaultOutput(moduleDir);

        List<File> listedDirs = lookupListedDirs(sourceSets);

        return new NbJavaModule(properties, compatibilityModel, sourceSets, listedDirs);
    }

    public static Map<File, NbJavaModel> parseFromIdeaModel(File projectDir, IdeaProject ideaModel) throws IOException {
        IdeaModule mainModule = GradleModelLoader.tryFindMainModule(projectDir, ideaModel);
        if (mainModule == null) {
            throw new IOException("Unable to find the main project in the model.");
        }

        DomainObjectSet<? extends IdeaModule> modules = ideaModel.getModules();
        Map<String, IdeaDependencyBuilder> cache = new HashMap<String, IdeaDependencyBuilder>(2 * modules.size());

        NbJavaModule parsedMainModule = tryParseModule(mainModule, cache);
        if (parsedMainModule == null) {
            throw new IOException("Unable to parse the main project from the model.");
        }

        Map<File, NbJavaModel> result = new HashMap<File, NbJavaModel>(2 * modules.size());
        NbJavaModel mainModel = new NbJavaModel(parsedMainModule);
        result.put(mainModel.getMainModule().getModuleDir(), mainModel);

        for (IdeaModule module: modules) {
            NbJavaModule parsedModule = tryParseModule(module, cache);
            if (parsedModule != null && !parsedMainModule.getUniqueName().equals(parsedModule.getUniqueName())) {
                File moduleDir = parsedModule.getModuleDir();
                if (moduleDir != null) {
                    NbJavaModel javaModel = new NbJavaModel(parsedModule);
                    result.put(javaModel.getMainModule().getModuleDir(), javaModel);
                }
            }
        }

        return result;
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
            this.mainCompile = new LinkedHashSet<File>();
            this.mainRuntime = new LinkedHashSet<File>();
            this.testCompile = new LinkedHashSet<File>();
            this.testRuntime = new LinkedHashSet<File>();
        }

        public void addAll(IdeaDependencyType type, IdeaDependencyBuilder dependencies) {
            switch (type) {
                case COMPILE:
                    addMainCompile(dependencies);
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

    private static final class NbOutput {
        private final File buildDir;
        private final File testBuildDir;

        public NbOutput(File buildDir, File testBuildDir) {
            if (buildDir == null) throw new NullPointerException("buildDir");
            if (testBuildDir == null) throw new NullPointerException("testBuildDir");

            this.buildDir = buildDir;
            this.testBuildDir = testBuildDir;
        }

        public File getBuildDir() {
            return buildDir;
        }

        public File getTestBuildDir() {
            return testBuildDir;
        }
    }

    private IdeaJavaModelUtils() {
        throw new AssertionError();
    }
}
