package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.BuiltInModelBuilder;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.GradleProjectInfoQuery;
import org.netbeans.gradle.model.GradleProjectTree;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.ProjectConnectionTask;
import org.netbeans.gradle.model.util.SourceSetVerification;
import org.netbeans.gradle.model.util.ZipUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.model.util.TestUtils.*;

public class MultiLevelJavaProjectTest {
    private static File tempFolder = null;
    private static File testedProjectDir = null;

    @BeforeClass
    public static void setUpClass() throws IOException {
        tempFolder = ZipUtils.unzipResourceToTemp(MultiLevelJavaProjectTest.class, "gradle-multi-level.zip");
        testedProjectDir = new File(tempFolder, "gradle-multi-level");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (tempFolder != null) {
            ZipUtils.recursiveDelete(tempFolder);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static File getSubPath(File root, String... subprojectNames) throws IOException {
        File result = root;
        for (String subprojectName: subprojectNames) {
            result = new File(result, subprojectName);
        }
        return result.getCanonicalFile();
    }

    private static File getProjectDir(String... subprojectNames) throws IOException {
        return getSubPath(testedProjectDir, subprojectNames);
    }

    private static <T> GradleProjectInfoQuery<T> toQuery(final ProjectInfoBuilder<T> builder) {
        return new GradleProjectInfoQuery<T>() {
            public ProjectInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.emptySet();
            }
        };
    }

    private static <T> GradleBuildInfoQuery<T> toQuery(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.singleton(ClassLoaderUtils.findClassPathOfClass(builder.getClass()));
            }
        };
    }

    private static GenericModelFetcher projectInfoFetcher(ProjectInfoBuilder<?>... builders) {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = new HashMap<Object, GradleProjectInfoQuery<?>>();
        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            projectInfos.put(i, toQuery(builders[i]));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    private static GenericModelFetcher basicInfoFetcher() {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = Collections.emptySet();

        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    private void runTestForSubProject(String projectName, ProjectConnectionTask task) {
        try {
            File subDir;
            if (projectName.length() > 0) {
                String relName = projectName.replace(":", File.separator);
                subDir = new File(testedProjectDir, relName);
            }
            else {
                subDir = testedProjectDir;
            }

            runTestsForProject(subDir, task);
        } catch (Throwable ex) {
            AssertionError error = new AssertionError("Failure for project \":" + projectName + "\": "
                    + ex.getMessage());
            error.initCause(ex);
            throw error;
        }
    }

    private static <T> T fetchSingleProjectInfo(
            ProjectConnection connection,
            ProjectInfoBuilder<T> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = projectInfoFetcher(infoBuilder);
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        assertTrue(models.getBuildInfoResults().isEmpty());

        @SuppressWarnings("unchecked")
        T result = (T)models.getDefaultProjectModels().getProjectInfoResults().get(0);
        return result;
    }

    private static GradleMultiProjectDef fetchProjectDef(
            ProjectConnection connection) throws IOException {

        GenericModelFetcher modelFetcher = basicInfoFetcher();
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        return models.getDefaultProjectModels().getProjectDef();
    }

    private static Set<String> toTaskNames(Collection<GradleTaskID> tasks) {
        Set<String> result = new LinkedHashSet<String>(2 * tasks.size());
        for (GradleTaskID task: tasks) {
            result.add(task.getName());
        }
        return result;
    }

    private static Set<String> mustHaveTasks(
            String relativeProjectPath,
            Collection<GradleTaskID> tasks,
            String... expectedNames) {
        String taskPrefix = ":" + relativeProjectPath + ":";

        Set<String> otherTasks = new LinkedHashSet<String>();

        Set<String> expectedSet = new LinkedHashSet<String>(Arrays.asList(expectedNames));
        for (GradleTaskID task: tasks) {
            String name = task.getName();
            assertEquals(taskPrefix + name, task.getFullName());

            if (!expectedSet.remove(name)) {
                otherTasks.add(name);
            }
        }

        if (!expectedSet.isEmpty()) {
            fail("The following tasks were not found but were expected: " + expectedSet
                    + ". The project has the following tasks: " + toTaskNames(tasks));
        }

        return otherTasks;
    }

    private static void testBasicInfoForProject(
            String relativeProjectName,
            GradleMultiProjectDef projectDef) throws IOException {
        assertNotNull("Must have a GradleMultiProjectDef.", projectDef);

        String[] projectPathParts = relativeProjectName.length() > 0
                ? relativeProjectName.split(Pattern.quote(":"))
                : new String[0];

        String projectPath = ":" + relativeProjectName;
        String projectName = projectPathParts.length > 0
                ? projectPathParts[projectPathParts.length - 1]
                : "gradle-multi-level";

        GradleProjectTree projectTree = projectDef.getMainProject();
        GenericProjectProperties genericProperties = projectTree.getGenericProperties();

        String fullName = genericProperties.getProjectFullName();
        assertEquals(projectPath, fullName);

        String simpleName = genericProperties.getProjectName();
        assertEquals(projectName, simpleName);

        File projectDir = genericProperties.getProjectDir();
        assertEquals(getProjectDir(projectPathParts), projectDir.getCanonicalFile());
    }

    private void testBasicInfoForProjectWithTasks(
            final String relativeProjectName,
            final String[] expectedTasks,
            final String[] unexpectedTasks) {

        runTestForSubProject(relativeProjectName, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                GradleMultiProjectDef projectDef = fetchProjectDef(connection);
                testBasicInfoForProject(relativeProjectName, projectDef);

                Collection<GradleTaskID> tasks = projectDef.getMainProject().getTasks();
                Set<String> remainingTasks = mustHaveTasks(relativeProjectName, tasks, expectedTasks);

                for (String task: unexpectedTasks) {
                    if (remainingTasks.contains(task)) {
                        fail("The project must not have the following task: " + task);
                    }
                }
            }
        });
    }

    private static String[] groovyProjects() {
        return new String[] {
            "apps:app1",
        };
    }

    private static String[] javaProjects() {
        return new String[] {
            "libs:lib3:lib1",
            "apps:app2",
            "libs:lib1",
            "libs:lib2",
            "libs:lib3",
            "libs:lib3:lib2",
        };
    }

    private static String[] concatArrays(String[]... arrays) {
        int length = 0;
        for (String[] array: arrays) {
            length += array.length;
        }

        String[] result = new String[length];
        int offset = 0;
        for (String[] array: arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private static String[] subprojects() {
        return concatArrays(groovyProjects(), javaProjects());
    }

    private static String[] allProjects() {
        return concatArrays(new String[]{""}, subprojects());
    }

    @Test
    public void testBasicInfoForJavaProjects() {
        String[] expectedTasks = {"clean", "build", "compileJava"};
        String[] unexpectedTasks = {"wrapper"};

        for (String relativeProjectName: javaProjects()) {
            testBasicInfoForProjectWithTasks(relativeProjectName, expectedTasks, unexpectedTasks);
        }
    }

    @Test
    public void testBasicInfoForGroovyProjects() {
        String[] expectedTasks = {"clean", "build", "compileJava", "compileGroovy"};
        String[] unexpectedTasks = {"wrapper"};

        for (String relativeProjectName: groovyProjects()) {
            testBasicInfoForProjectWithTasks(relativeProjectName, expectedTasks, unexpectedTasks);
        }
    }

    @Test
    public void testBasicInfoForRootProject() {
        String[] expectedTasks = {};
        String[] unexpectedTasks = {"compileJava"};

        testBasicInfoForProjectWithTasks("", expectedTasks, unexpectedTasks);
    }

    private static JavaClassPaths classPathsOfSourceSet(JavaSourcesModel model, String sourceSetName) {
        for (JavaSourceSet sourceSet: model.getSourceSets()) {
            if (sourceSetName.equals(sourceSet.getName())) {
                return sourceSet.getClasspaths();
            }
        }

        throw new AssertionError("Missing source set: " + sourceSetName);
    }

    private Map<File, String> parseProjectDependencies(String... projectDependencies) throws IOException {
        Map<File, String> result = new HashMap<File, String>(2 * projectDependencies.length);
        for (String dep: projectDependencies) {
            String[] nameParts = dep.split(Pattern.quote(":"));
            String name = nameParts[nameParts.length - 1];
            File projectDir = getProjectDir(nameParts);
            result.put(getSubPath(projectDir, "build", "libs", name + ".jar"), dep);
        }
        return result;
    }

    private void verifyProjectDependencies(Set<File> files, String... projectDependencies) throws IOException {
        Map<File, String> expected = parseProjectDependencies(projectDependencies);
        for (Map.Entry<File, String> entry: expected.entrySet()) {
            if (!files.contains(entry.getKey())) {
                fail("Expected project dependency " + entry.getValue());
            }
        }
    }

    private void verifyArtifact(Set<File> files, String artifactName, String version) {
        for (File file: files) {
            String name = file.getName();
            if (name.contains(artifactName) && name.contains(version)) {
                return;
            }
        }

        fail("Missing required artifact: " + artifactName + ":" + version);
    }

    private void verifyTestClassPath(Set<File> files) {
        verifyArtifact(files, "junit", "4.11");
        verifyArtifact(files, "mockito-core", "1.9.5");
    }

    private void testJavaSourcesModelForJavaProject(
            String relativeProjectName,
            final JavaSourcesModel expected,
            final String... projectDependencies) {

        runTestForSubProject(relativeProjectName, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaSourcesModel sourcesModel
                        = fetchSingleProjectInfo(connection, JavaSourcesModelBuilder.COMPLETE);
                assertNotNull("Must have a JavaSourcesModel.", sourcesModel);
                SourceSetVerification.verifySourcesModelWithoutDependencies(expected, sourcesModel);

                JavaClassPaths mainClassPaths = classPathsOfSourceSet(sourcesModel, "main");
                verifyProjectDependencies(mainClassPaths.getCompileClasspaths(), projectDependencies);
                verifyProjectDependencies(mainClassPaths.getRuntimeClasspaths(), projectDependencies);

                JavaClassPaths testClassPaths = classPathsOfSourceSet(sourcesModel, "test");
                verifyTestClassPath(testClassPaths.getCompileClasspaths());
                verifyTestClassPath(testClassPaths.getRuntimeClasspaths());
            }
        });
    }

    @Test
    public void testJavaSourcesModel() throws IOException {
        testJavaSourcesModelForJavaProject("apps:app1", sourcesOfApp1(), "libs:lib1", "libs:lib2");
        testJavaSourcesModelForJavaProject("apps:app2", sourcesOfApp2(), "libs:lib1", "libs:lib2");
        testJavaSourcesModelForJavaProject("libs:lib1", sourcesOfLib1(), "libs:lib2");
        testJavaSourcesModelForJavaProject("libs:lib2", sourcesOfLib2());
        testJavaSourcesModelForJavaProject("libs:lib3", sourcesOfLib3());
        testJavaSourcesModelForJavaProject("libs:lib3:lib1", sourcesOfLib3Lib1());
        testJavaSourcesModelForJavaProject("libs:lib3:lib2", sourcesOfLib3Lib2());
    }

    @Test
    public void testJavaSourcesModelForRoot() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaSourcesModel sourcesModel
                        = fetchSingleProjectInfo(connection, JavaSourcesModelBuilder.ONLY_COMPILE);
                assertNull("Root must not have a JavaSourcesModel.", sourcesModel);
            }
        });
    }

    private void testJavaCompatibilityModel(String relativeProjectName) throws IOException {
        runTestForSubProject(relativeProjectName, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaCompatibilityModel compatibilityModel
                        = fetchSingleProjectInfo(connection, JavaCompatibilityModelBuilder.INSTANCE);
                assertNotNull("Must have a JavaCompatibilityModel.", compatibilityModel);

                assertEquals("1.5", compatibilityModel.getSourceCompatibility());
                assertEquals("1.7", compatibilityModel.getTargetCompatibility());
            }
        });
    }

    @Test
    public void testJavaCompatibilityModel() throws IOException {
        for (String project: subprojects()) {
            testJavaCompatibilityModel(project);
        }
    }

    @Test
    public void testJavaCompatibilityModelForRoot() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaCompatibilityModel compatibilityModel
                        = fetchSingleProjectInfo(connection, JavaCompatibilityModelBuilder.INSTANCE);
                assertNull("Root must not have a JavaCompatibilityModel.", compatibilityModel);
            }
        });
    }

    private void testJarOutputsModel(String relativeProjectPath) throws IOException {
        String[] projectPathParts = relativeProjectPath.split(Pattern.quote(":"));
        String name = projectPathParts[projectPathParts.length - 1];
        File projectDir = getProjectDir(projectPathParts);
        final File expectedJarPath = getSubPath(projectDir, "build", "libs", name + ".jar");

        runTestForSubProject(relativeProjectPath, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JarOutputsModelBuilder.INSTANCE);
                assertNotNull("Must have a JarOutputsModel.", jarOutputs);

                JarOutput mainJar = null;
                for (JarOutput jar: jarOutputs.getJars()) {
                    if (jar.getTaskName().equals("jar")) {
                        mainJar = jar;
                        break;
                    }
                }

                assertNotNull("Project must contain a main jar.", mainJar);
                assertEquals("Output jar must be at the expected location",
                        expectedJarPath, mainJar.getJar().getCanonicalFile());
            }
        });
    }

    @Test
    public void testJarOutputsModel() throws IOException {
        for (String project: subprojects()) {
            testJarOutputsModel(project);
        }
    }

    @Test
    public void testJarOutputsModelForRoot() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JarOutputsModelBuilder.INSTANCE);
                assertNull("Root project must not have a JarOutputsModel.", jarOutputs);
            }
        });
    }

    @Test
    public void testWarFoldersModel() throws IOException {
        for (String project: allProjects()) {
            runTestForSubProject(project, new ProjectConnectionTask() {
                public void doTask(ProjectConnection connection) throws Exception {
                    WarFoldersModel warFolders
                            = fetchSingleProjectInfo(connection, WarFoldersModelBuilder.INSTANCE);
                    assertNull("Must not have a WarFoldersModel.", warFolders);
                }
            });
        }
    }

    private static Map<Class<?>, Object> fetchBuiltInModels(
            ProjectConnection connection,
            Class<?>... modelClasses) throws IOException {

        Map<Object, GradleBuildInfoQuery<?>> buildInfos = new HashMap<Object, GradleBuildInfoQuery<?>>();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = Collections.emptySet();

        buildInfos.put(0, toQuery(new BuiltInModelBuilder(modelClasses)));

        GenericModelFetcher modelFetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        @SuppressWarnings("unchecked")
        Map<Class<?>, Object> result = (Map<Class<?>, Object>)models.getBuildInfoResults().get(0);
        return result;
    }

    private void testBuiltInModels(String relativeProjectPath) throws IOException {
        final String projectPath = ":" + relativeProjectPath;

        runTestForSubProject(relativeProjectPath, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                Class<?>[] models = new Class<?>[]{
                    EclipseProject.class,
                    IdeaProject.class,
                    GradleProject.class
                };

                Map<Class<?>, Object> fetched = fetchBuiltInModels(connection, models);

                HashSet<Class<?>> expected = new HashSet<Class<?>>(Arrays.asList(models));
                expected.removeAll(fetched.keySet());

                if (!expected.isEmpty()) {
                    fail("The following models are unavailable: "
                            + expected.toString().replace(",", ",\n"));
                }

                // FIXME: We don't test the GradleProject instance because
                //  Gradle returns the root project for each project. Is this a bug?

                EclipseProject eclipseProject = (EclipseProject)fetched.get(EclipseProject.class);
                assertEquals("EclipseProject must match the requested one",
                        projectPath, eclipseProject.getGradleProject().getPath());

                IdeaProject ideaProject = (IdeaProject)fetched.get(IdeaProject.class);
                GradleProject gradleProjectOfIdea = null;
                for (IdeaModule ideaModule: ideaProject.getModules()) {
                    if (projectPath.equals(ideaModule.getGradleProject().getPath())) {
                        gradleProjectOfIdea = ideaModule.getGradleProject();
                        break;
                    }
                }

                assertNotNull("IdeaProject must contain the requested module.", gradleProjectOfIdea);
            }
        });
    }

    @Test
    public void testBuiltInModels() throws IOException {
        for (String project: allProjects()) {
            testBuiltInModels(project);
        }
    }

    private static JavaOutputDirs defaultOutputOfSourceSet(
            File projectDir,
            String name) throws IOException {

        File classesDir = getSubPath(projectDir, "build", "classes", name);
        File resourcesDir = getSubPath(projectDir, "build", "resources", name);
        return new JavaOutputDirs(classesDir, resourcesDir, Collections.<File>emptySet());
    }

    private static JavaSourceGroup sourceGroupOfSourceSet(
            File projectDir,
            String name,
            JavaSourceGroupName sourceGroupName) throws IOException {

        File sourceRoot = getSubPath(projectDir, name, sourceGroupName.name().toLowerCase(Locale.US));
        return new JavaSourceGroup(sourceGroupName, Collections.singleton(sourceRoot));
    }

    private static JavaSourceSet.Builder defaultSourceSetBuilder(
            File projectDir,
            String name) throws IOException {

        JavaSourceSet.Builder sourceSet = new JavaSourceSet.Builder(
                name,
                defaultOutputOfSourceSet(projectDir, name));

        sourceSet.addSourceGroup(sourceGroupOfSourceSet(projectDir, name, JavaSourceGroupName.JAVA));
        sourceSet.addSourceGroup(sourceGroupOfSourceSet(projectDir, name, JavaSourceGroupName.RESOURCES));

        return sourceSet;
    }

    private static File projectDirByRelativeName(String relativeProjectName) throws IOException {
        String[] projectParts = relativeProjectName.length() > 0
                ? relativeProjectName.split(Pattern.quote(":"))
                : new String[0];
        return getProjectDir(projectParts);
    }

    private static JavaSourceSet defaultJavaSourceSet(
            File projectDir,
            String name) throws IOException {

        JavaSourceSet.Builder builder = defaultSourceSetBuilder(projectDir, name);
        return builder.create();
    }

    private static JavaSourceSet defaultGroovySourceSet(
            File projectDir,
            String name) throws IOException {

        JavaSourceSet.Builder builder = defaultSourceSetBuilder(projectDir, name);
        builder.addSourceGroup(sourceGroupOfSourceSet(projectDir, name, JavaSourceGroupName.GROOVY));

        return builder.create();
    }

    private static JavaSourcesModel sourcesOfJavaProject(String relativeProjectName) throws IOException {
        File projectDir = projectDirByRelativeName(relativeProjectName);

        Collection<JavaSourceSet> sourceSets = new LinkedList<JavaSourceSet>();
        sourceSets.add(defaultJavaSourceSet(projectDir, "main"));
        sourceSets.add(defaultJavaSourceSet(projectDir, "test"));

        return new JavaSourcesModel(sourceSets);
    }

    private static JavaSourcesModel sourcesOfApp1() throws IOException {
        String relativeProjectName = "apps:app1";
        File projectDir = projectDirByRelativeName(relativeProjectName);

        Collection<JavaSourceSet> sourceSets = new LinkedList<JavaSourceSet>();
        sourceSets.add(defaultGroovySourceSet(projectDir, "main"));
        sourceSets.add(defaultGroovySourceSet(projectDir, "test"));

        return new JavaSourcesModel(sourceSets);
    }

    private static JavaSourcesModel sourcesOfApp2() throws IOException {
        return sourcesOfJavaProject("apps:app2");
    }

    private static JavaSourcesModel sourcesOfLib1() throws IOException {
        return sourcesOfJavaProject("libs:lib1");
    }

    private static JavaSourcesModel sourcesOfLib2() throws IOException {
        return sourcesOfJavaProject("libs:lib2");
    }

    private static JavaSourcesModel sourcesOfLib3() throws IOException {
        return sourcesOfJavaProject("libs:lib3");
    }

    private static JavaSourcesModel sourcesOfLib3Lib1() throws IOException {
        return sourcesOfJavaProject("libs:lib3:lib1");
    }

    private static JavaSourcesModel sourcesOfLib3Lib2() throws IOException {
        return sourcesOfJavaProject("libs:lib3:lib2");
    }
}
