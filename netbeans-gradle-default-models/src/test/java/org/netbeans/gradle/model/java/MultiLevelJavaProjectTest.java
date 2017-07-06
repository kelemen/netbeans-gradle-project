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
import java.util.List;
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
import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.BuilderResult;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedModelsOrError;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.GradleProjectTree;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.internal.ConstBuilders;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.ProjectConnectionTask;
import org.netbeans.gradle.model.util.SourceSetVerification;
import org.netbeans.gradle.model.util.TestUtils;
import org.netbeans.gradle.model.util.ZipUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.model.java.InfoQueries.*;
import static org.netbeans.gradle.model.util.TestUtils.*;

public class MultiLevelJavaProjectTest {
    private static final String ROOT_NAME = "gradle-multi-level";

    private static File tempFolder = null;
    private static File testedProjectDir = null;

    @BeforeClass
    public static void setUpClass() throws IOException {
        tempFolder = ZipUtils.unzipResourceToTemp(MultiLevelJavaProjectTest.class, "gradle-multi-level.zip");
        testedProjectDir = new File(tempFolder, ROOT_NAME);
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

    private static File getProjectDir(String... subprojectNames) throws IOException {
        return getSubPath(testedProjectDir, subprojectNames);
    }

    private void runTestForSubProject(String projectName, ProjectConnectionTask task) {
        TestUtils.runTestForSubProject(testedProjectDir, projectName, task);
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
        String taskPrefix = relativeProjectPath.length() > 0
                ? ":" + relativeProjectPath + ":"
                : ":";

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
                : ROOT_NAME;

        GradleProjectTree projectTree = projectDef.getMainProject();
        GenericProjectProperties genericProperties = projectTree.getGenericProperties();

        String fullName = genericProperties.getProjectFullName();
        assertEquals(projectPath, fullName);

        String simpleName = genericProperties.getProjectName();
        assertEquals(projectName, simpleName);

        ProjectId projectId = genericProperties.getProjectId();
        assertEquals("group", "my-group", projectId.getGroup());
        assertEquals("name", simpleName, projectId.getName());
        assertEquals("version", getProjectVersion(simpleName), projectId.getVersion());

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

                GradleProjectTree mainProject = projectDef.getMainProject();
                GenericProjectProperties genericProperties = mainProject.getGenericProperties();

                assertEquals("Build script for the project must be build.gradle.",
                        new File(genericProperties.getProjectDir(), "build.gradle"),
                        genericProperties.getBuildScript());

                Collection<GradleTaskID> tasks = mainProject.getTasks();
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

    private static void assertNoProblem(Throwable issue) {
        if (issue != null) {
            throw Exceptions.throwUnchecked(issue);
        }
    }

    private static JavaClassPaths classPathsOfSourceSet(JavaSourcesModel model, String sourceSetName) {
        for (JavaSourceSet sourceSet: model.getSourceSets()) {
            if (sourceSetName.equals(sourceSet.getName())) {
                assertNoProblem(sourceSet.getCompileClassPathProblem());
                assertNoProblem(sourceSet.getRuntimeClassPathProblem());
                return sourceSet.getClasspaths();
            }
        }

        throw new AssertionError("Missing source set: " + sourceSetName);
    }

    private static File getLibraryPath(File projectDir) throws IOException {
        return getSubPath(projectDir, "build", "libs");
    }

    private static File getFileInLibs(File projectDir, String... subPaths) throws IOException {
        return getSubPath(getLibraryPath(projectDir), subPaths);
    }

    private Map<File, String> parseProjectDependencies(String... projectDependencies) throws IOException {
        Map<File, String> result = CollectionUtils.newHashMap(projectDependencies.length);
        for (String dep: projectDependencies) {
            String[] nameParts = dep.split(Pattern.quote(":"));
            String name = nameParts[nameParts.length - 1];
            File projectDir = getProjectDir(nameParts);
            result.put(getFileInLibs(projectDir, name + "-" + getProjectVersion(name) + ".jar"), dep);
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
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_SOURCES_BUILDER_COMPLETE);
                assertNotNull("Must have a JavaSourcesModel.", sourcesModel);
                SourceSetVerification.verifySourcesModelWithoutDependencies(expected, sourcesModel);

                JavaClassPaths mainClassPaths = classPathsOfSourceSet(sourcesModel, "main");
                verifyProjectDependencies(mainClassPaths.getCompileClasspaths(), projectDependencies);
                verifyProjectDependencies(mainClassPaths.getRuntimeClasspaths(), projectDependencies);

                JavaClassPaths testClassPaths = classPathsOfSourceSet(sourcesModel, "test");
                verifyTestClassPath(testClassPaths.getCompileClasspaths());
                verifyTestClassPath(testClassPaths.getRuntimeClasspaths());

                JavaModelTests.checkNoDependencyResolultionError(sourcesModel);
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

    private static JavaSourceGroup findSourceGroup(JavaSourceSet sourceSet, JavaSourceGroupName name) {
        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            if (name.equals(group.getGroupName())) {
                return group;
            }
        }

        return null;
    }

    private static JavaSourceSet findSourceSet(JavaSourcesModel sourcesModel, String name) {
        for (JavaSourceSet sourceSet: sourcesModel.getSourceSets()) {
            if (name.equals(sourceSet.getName())) {
                return sourceSet;
            }
        }

        return null;
    }

    private static JavaSourceGroup findSourceGroup(
            JavaSourcesModel sourcesModel,
            String setName,
            JavaSourceGroupName groupName) {
        JavaSourceSet sourceSet = findSourceSet(sourcesModel, setName);
        if (sourceSet == null) {
            return null;
        }

        return findSourceGroup(sourceSet, groupName);
    }

    @Test
    public void testExcludeIncludePatterns() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaSourcesModel sourcesModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_SOURCES_BUILDER_ONLY_COMPILE);
                assertNotNull("apps:app1 must have a JavaSourcesModel.", sourcesModel);

                JavaSourceGroup group = findSourceGroup(sourcesModel, JavaSourceSet.NAME_MAIN, JavaSourceGroupName.JAVA);
                assertNotNull("apps:app1 must not have a main/java source group.", group);

                SourceIncludePatterns patterns = group.getExcludePatterns();
                assertEquals("includes", Collections.singleton("**"), patterns.getIncludePatterns());
                assertEquals("excludes", Collections.singleton("**/excluded/"), patterns.getExcludePatterns());
            }
        });
    }

    @Test
    public void testJavaSourcesModelForRoot() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaSourcesModel sourcesModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_SOURCES_BUILDER_ONLY_COMPILE);
                assertNull("Root must not have a JavaSourcesModel.", sourcesModel);
            }
        });
    }

    private void testJavaCompatibilityModel(String relativeProjectName) throws IOException {
        runTestForSubProject(relativeProjectName, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaCompatibilityModel compatibilityModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER);
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
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER);
                assertNull("Root must not have a JavaCompatibilityModel.", compatibilityModel);
            }
        });
    }

    @Test
    public void testMissingJacocoPluginIsNotAProblem() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JacocoModel jacocoModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JACOCO_BUILDER);
                assertNull("apps:app1 must not have a JacocoModel.", jacocoModel);
            }
        });
    }

    private static String getProjectVersion(String name) {
        return "app1".equals(name) ? "5.95.3-beta" : "3.5.78-alpha";
    }

    private void testJarOutputsModel(String relativeProjectPath) throws IOException {
        String[] projectPathParts = relativeProjectPath.split(Pattern.quote(":"));
        String name = projectPathParts[projectPathParts.length - 1];
        File projectDir = getProjectDir(projectPathParts);
        final File expectedJarPath = getFileInLibs(projectDir, name + "-" + getProjectVersion(name) + ".jar");

        runTestForSubProject(relativeProjectPath, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAR_OUTPUTS_BUILDER);
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
                assertNull("mainJar.tryGetSourceSetNames", mainJar.tryGetSourceSetNames());
            }
        });
    }

    @Test
    public void testJarOutputsModel() throws IOException {
        for (String project: subprojects()) {
            testJarOutputsModel(project);
        }
    }

    private static JarOutput getJarOutput(JarOutputsModel jarOutputs, String taskName) {
        for (JarOutput jar: jarOutputs.getJars()) {
            if (jar.getTaskName().equals(taskName)) {
                return jar;
            }
        }

        throw new AssertionError("Missing jar task: " + taskName);
    }

    @Test
    public void testCustomJarOutputsModel() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAR_OUTPUTS_BUILDER);
                assertNotNull("Must have a JarOutputsModel.", jarOutputs);

                File projectDir = getProjectDir("apps", "app1");
                String version = getProjectVersion("app1");

                JarOutput testJar = getJarOutput(jarOutputs, "testJar");
                assertEquals("Output jar must be at the expected location",
                        getFileInLibs(projectDir, "test-app1-" + version + ".jar"),
                        testJar.getJar().getCanonicalFile());
                assertEquals("testJar.name", "testJar", testJar.getTaskName());
                assertEquals("testJar.sourceSets",
                        Collections.singleton("test"),
                        testJar.tryGetSourceSetNames());

                JarOutput customJar = getJarOutput(jarOutputs, "customJar");
                assertEquals("Output jar must be at the expected location",
                        getFileInLibs(projectDir, "custom-app1-" + version + ".jar"),
                        customJar.getJar().getCanonicalFile());
                assertEquals("customJar.name", "customJar", customJar.getTaskName());
                assertEquals("customJar.sourceSets",
                        Collections.singleton("main"),
                        customJar.tryGetSourceSetNames());
            }
        });
    }

    private Map<String, JavaTestTask> nameToTestTask(JavaTestModel testModel) {
        Collection<JavaTestTask> testTasks = testModel.getTestTasks();
        Map<String, JavaTestTask> result = CollectionUtils.newHashMap(testTasks.size());
        for (JavaTestTask testTask: testTasks) {
            if (result.put(testTask.getName(), testTask) != null) {
                fail("Test task has been retrieved multiple times: " + testTask.getName());
            }
        }
        return result;
    }

    private void verifyTestTask(Map<String, JavaTestTask> testTasks, String name, File expectedOutputDir) {
        JavaTestTask testTask = testTasks.get(name);
        assertNotNull("Expected test task with name " + name, testTask);
        assertEquals(name, testTask.getName());
        assertEquals(expectedOutputDir, testTask.getXmlOutputDir());
    }

    private void testJavaTestModel(String relativeProjectPath) throws IOException {
        String[] projectPathParts = relativeProjectPath.split(Pattern.quote(":"));
        File projectDir = getProjectDir(projectPathParts);
        final File expectedXmlPathTest = getSubPath(projectDir, "build", "test-results");
        final File expectedXmlPathMyTest = getSubPath(projectDir, "build", "my-test-custom-results");

        runTestForSubProject(relativeProjectPath, new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaTestModel testModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_TEST_BUILDER);
                assertNotNull("Must have a JavaTestModel.", testModel);

                Map<String, JavaTestTask> testTasks = nameToTestTask(testModel);
                assertEquals("Must have a 2 test tasks.", 2, testTasks.size());

                verifyTestTask(testTasks, "test", expectedXmlPathTest);
                verifyTestTask(testTasks, "myTest", expectedXmlPathMyTest);
            }
        });
    }

    @Test
    public void testJavaTestModel() throws IOException {
        for (String project: subprojects()) {
            testJavaTestModel(project);
        }
    }

    @Test
    public void testJavaTestModelForRoot() throws IOException {
         runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaTestModel testModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAVA_TEST_BUILDER);
                assertNull("Root project must not have a JavaTestModel.", testModel);
            }
        });
    }

    @Test
    public void testJarOutputsModelForRoot() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JAR_OUTPUTS_BUILDER);
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
                            = fetchSingleProjectInfo(connection, JavaModelBuilders.WAR_FOLDERS_BUILDER);
                    assertNull("Must not have a WarFoldersModel.", warFolders);
                }
            });
        }
    }

    @Test
    public void testCustomQuery() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                String prefix = "testCustomQuery-";
                String result = fetchSingleBuildInfo(connection, TestBuilders.testBuildInfoBuilder(prefix));

                assertEquals(prefix + ROOT_NAME, result);
            }
        });
    }

    @Test
    public void testFailingProjectQuery() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                String message = "testFailingProjectQuery-message";
                ProjectInfoBuilder2<?> builder = TestBuilders.failingProjectInfoBuilder(message);
                BuilderResult result = fetchSingleProjectInfoWithError(connection, builder);
                assertNotNull("Required result for FailingProjectInfoBuilder.", result);

                BuilderIssue issue = result.getIssue();
                assertNotNull("Required issue for FailingProjectInfoBuilder", issue);

                assertEquals("Expected approriate builder name.", builder.getName(), issue.getName());
                String issueMessage = issue.getException().getMessage();
                if (!issueMessage.contains(message)) {
                    fail("Issue message is invalid: " + issueMessage);
                }
            }
        });
    }

    @Test
    public void testFailingBuildQuery() throws IOException {
        runTestForSubProject("", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                String message = "testFailingBuildQuery-message";
                BuildInfoBuilder<?> builder = TestBuilders.failingBuildInfoBuilder(message);

                BuilderResult result = fetchSingleBuildInfoWithError(connection, builder);
                assertNotNull("Required result for FailingBuildInfoBuilder.", result);

                BuilderIssue issue = result.getIssue();
                assertNotNull("Required issue for FailingBuildInfoBuilder", issue);

                assertEquals("Expected approriate builder name.", builder.getName(), issue.getName());
                String issueMessage = issue.getException().getMessage();
                if (!issueMessage.contains(message)) {
                    fail("Issue message is invalid: " + issueMessage);
                }
            }
        });
    }

    private static Object getSingleBuildResult(List<BuilderResult> list) {
        BuilderResult result = CollectionUtils.getSingleElement(list);
        return result != null ? result.getResultIfNoIssue() : null;
    }

    private static FetchedModels verifyNoError(FetchedModelsOrError modelsOrError) {
        return InfoQueries.verifyNoError(modelsOrError);
    }

    @Test
    public void testManyQueries() throws IOException {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = new HashMap<Object, List<GradleBuildInfoQuery<?>>>();

        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = new HashMap<Object, List<GradleProjectInfoQuery2<?>>>();

        Set<Class<?>> toolingModels = new HashSet<Class<?>>();

        projectInfos.put(0, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(JavaModelBuilders.JAR_OUTPUTS_BUILDER)));
        projectInfos.put(1, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER)));
        projectInfos.put(2, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(JavaModelBuilders.JAVA_SOURCES_BUILDER_COMPLETE)));
        projectInfos.put(3, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(JavaModelBuilders.WAR_FOLDERS_BUILDER)));

        final String prefix = "testCustomQuery-";
        buildInfos.put(0, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.testBuildInfoBuilder(prefix))));

        toolingModels.add(IdeaProject.class);

        final GenericModelFetcher fetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                FetchedModels models = verifyNoError(fetcher.getModels(connection, TestUtils.defaultInit()));

                String buildInfo = (String)getSingleBuildResult(
                        models.getBuildInfoResults().get(0));
                assertEquals(prefix + ROOT_NAME, buildInfo);

                Map<Object, List<BuilderResult>> projectInfos
                        = models.getDefaultProjectModels().getProjectInfoResults();

                JarOutputsModel model1 = (JarOutputsModel)getSingleBuildResult(projectInfos.get(0));
                JavaCompatibilityModel model2 = (JavaCompatibilityModel)getSingleBuildResult(projectInfos.get(1));
                JavaSourcesModel model3 = (JavaSourcesModel)getSingleBuildResult(projectInfos.get(2));
                WarFoldersModel model4 = (WarFoldersModel)getSingleBuildResult(projectInfos.get(3));

                assertNotNull(model1);
                assertNotNull(model2);
                assertNotNull(model3);
                assertNull(model4);
            }
        });
    }

    private static <T> T findResultOfType(Class<T> type, Collection<BuilderResult> builders) {
        for (BuilderResult builder: builders) {
            Object result = builder.getResultObject();
            if (type.isInstance(result)) {
                return type.cast(result);
            }
        }
        return null;
    }

    @Test
    public void testManyQueriesSingleKey() throws IOException {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = new HashMap<Object, List<GradleBuildInfoQuery<?>>>();

        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = new HashMap<Object, List<GradleProjectInfoQuery2<?>>>();

        Set<Class<?>> toolingModels = new HashSet<Class<?>>();

        projectInfos.put(0, Arrays.<GradleProjectInfoQuery2<?>>asList(
                InfoQueries.toCustomQuery(JavaModelBuilders.JAR_OUTPUTS_BUILDER),
                InfoQueries.toCustomQuery(JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER),
                InfoQueries.toCustomQuery(JavaModelBuilders.JAVA_SOURCES_BUILDER_COMPLETE),
                InfoQueries.toCustomQuery(JavaModelBuilders.WAR_FOLDERS_BUILDER)));

        final String prefix = "testCustomQuery-";
        buildInfos.put(0, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.testBuildInfoBuilder(prefix))));

        toolingModels.add(IdeaProject.class);

        final GenericModelFetcher fetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                FetchedModels models = verifyNoError(fetcher.getModels(connection, TestUtils.defaultInit()));

                String buildInfo = (String)getSingleBuildResult(
                        models.getBuildInfoResults().get(0));
                assertEquals(prefix + ROOT_NAME, buildInfo);

                Map<Object, List<BuilderResult>> projectInfos
                        = models.getDefaultProjectModels().getProjectInfoResults();

                List<BuilderResult> results = projectInfos.get(0);
                assertNotNull("Must have results of query", results);

                assertEquals("Must have 3/4 queries.", 3, results.size());

                JarOutputsModel model1 = findResultOfType(JarOutputsModel.class, results);
                JavaCompatibilityModel model2 = findResultOfType(JavaCompatibilityModel.class, results);
                JavaSourcesModel model3 = findResultOfType(JavaSourcesModel.class, results);
                WarFoldersModel model4 = findResultOfType(WarFoldersModel.class, results);

                assertNotNull(model1);
                assertNotNull(model2);
                assertNotNull(model3);
                assertNull(model4);
            }
        });
    }

    private static void verifySerializationError(BuilderResult result) {
        assertNotNull("Must have a result with a serialization issue.", result);

        BuilderIssue issue = result.getIssue();
        assertNotNull("Must have a serialization issue.", issue);

        assertTrue("Cause must be a serialization error.",
                issue.getException().getMessage().contains("NotSerializableException"));
    }

    private static void verifyDeserializationError(BuilderResult result) {
        assertNotNull("Must have a result with a deserialization issue.", result);

        BuilderIssue issue = result.getIssue();
        assertNotNull("Must have a deserialization issue.", issue);

        assertTrue("Cause must be a ClassNotFoundException error.",
                issue.getException().getMessage().contains("ClassNotFoundException"));
    }

    @Test
    public void testEvenBuilderNameFails() throws IOException {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos = Collections.emptyMap();

        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = new HashMap<Object, List<GradleProjectInfoQuery2<?>>>();

        final String infoMessage = "TEST-INFO-1270748702750";
        final String nameMessage = "TEST-NAME-5478210972357";
        projectInfos.put(1, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.failingNameProjectInfoBuilder(infoMessage, nameMessage))));

        Set<Class<?>> toolingModels = Collections.emptySet();

        final GenericModelFetcher fetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                FetchedModels models = verifyNoError(fetcher.getModels(connection, TestUtils.defaultInit()));
                BuilderResult result = CollectionUtils.getSingleElement(
                        models.getDefaultProjectModels().getProjectInfoResults().get(1));

                assertNotNull("Required result for FailingProjectInfoBuilder.", result);

                BuilderIssue issue = result.getIssue();
                assertNotNull("Required issue for FailingProjectInfoBuilder", issue);

                //assertEquals("Expected approriate builder name.", builder.getName(), issue.getName());
                String issueMessage = issue.getException().getMessage();
                if (!issueMessage.contains(infoMessage)) {
                    fail("Issue message is invalid: " + issueMessage);
                }
            }
        });
    }

    @Test
    public void testSerializationFailures() throws IOException {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = new HashMap<Object, List<GradleBuildInfoQuery<?>>>();

        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = new HashMap<Object, List<GradleProjectInfoQuery2<?>>>();

        Set<Class<?>> toolingModels = new HashSet<Class<?>>();

        final String projectInfoPrefix = "testSerializationFailures-project-";
        final String buildInfoPrefix = "testSerializationFailures-build-";

        projectInfos.put(0, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.testProjectInfoBuilder(projectInfoPrefix))));
        projectInfos.put(1, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.notSerializableProjectInfoBuilder())));
        projectInfos.put(2, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.notSerializableResultProjectInfoBuilder())));
        projectInfos.put(3, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toQueryWithKnownClassPath(TestBuilders.testProjectInfoBuilder(""))));
        projectInfos.put(4, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                InfoQueries.toQueryWithKnownClassPath(ConstBuilders.constProjectInfoBuilder(new SerializableObject()))));

        buildInfos.put(0, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.testBuildInfoBuilder(buildInfoPrefix))));
        buildInfos.put(1, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.notSerializableBuildInfoBuilder())));
        buildInfos.put(2, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toCustomQuery(TestBuilders.notSerializableResultBuildInfoBuilder())));
        buildInfos.put(3, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toQueryWithKnownClassPath(TestBuilders.testBuildInfoBuilder(""))));
        buildInfos.put(4, Collections.<GradleBuildInfoQuery<?>>singletonList(
                InfoQueries.toQueryWithKnownClassPath(ConstBuilders.constBuildInfoBuilder(new SerializableObject()))));

        toolingModels.add(IdeaProject.class);

        final GenericModelFetcher fetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                FetchedModels models = verifyNoError(fetcher.getModels(connection, TestUtils.defaultInit()));

                BuilderResult buildInfo1 = CollectionUtils
                        .getSingleElement(models.getBuildInfoResults().get(1));
                BuilderResult buildInfo2 = CollectionUtils
                        .getSingleElement(models.getBuildInfoResults().get(2));
                BuilderResult buildInfo3 = CollectionUtils
                        .getSingleElement(models.getBuildInfoResults().get(3));
                BuilderResult buildInfo4 = CollectionUtils
                        .getSingleElement(models.getBuildInfoResults().get(4));

                verifySerializationError(buildInfo1);
                verifySerializationError(buildInfo2);
                verifyDeserializationError(buildInfo3);
                verifyDeserializationError(buildInfo4);

                BuilderResult projectInfo1 = CollectionUtils.getSingleElement(
                        models.getDefaultProjectModels().getProjectInfoResults().get(1));
                BuilderResult projectInfo2 = CollectionUtils.getSingleElement(
                        models.getDefaultProjectModels().getProjectInfoResults().get(2));
                BuilderResult projectInfo3 = CollectionUtils.getSingleElement(
                        models.getDefaultProjectModels().getProjectInfoResults().get(3));
                BuilderResult projectInfo4 = CollectionUtils.getSingleElement(
                        models.getDefaultProjectModels().getProjectInfoResults().get(4));

                verifySerializationError(projectInfo1);
                verifySerializationError(projectInfo2);
                verifyDeserializationError(projectInfo3);
                verifyDeserializationError(projectInfo4);

                Object buildInfoOk = getSingleBuildResult(
                        models.getBuildInfoResults().get(0));
                assertTrue("The valid builder must succeed",
                        buildInfoOk.toString().startsWith(buildInfoPrefix));

                Object projectInfoOk = getSingleBuildResult(
                        models.getDefaultProjectModels().getProjectInfoResults().get(0));
                assertTrue("The valid builder must succeed",
                        projectInfoOk.toString().startsWith(projectInfoPrefix));
            }
        });
    }

    private static Map<Class<?>, Object> fetchBuiltInModels(
            ProjectConnection connection,
            Class<?>... modelClasses) throws IOException {

        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos = Collections.emptyMap();
        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = new HashSet<Class<?>>(Arrays.asList(modelClasses));

        GenericModelFetcher modelFetcher = new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
        FetchedModels models = verifyNoError(modelFetcher.getModels(connection, defaultInit()));

        return models.getDefaultProjectModels().getToolingModels();
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

        File classesDir = getSubPath(projectDir, "build", "myclasses", name);
        File resourcesDir = getSubPath(projectDir, "build", "myresources", name);
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
        return getSubProjectDir(testedProjectDir, relativeProjectName);
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
