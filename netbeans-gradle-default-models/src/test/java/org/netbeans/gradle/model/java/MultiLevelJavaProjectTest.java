package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
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

    private static File getProjectDir(String... subprojectNames) throws IOException {
        File result = testedProjectDir;
        for (String subprojectName: subprojectNames) {
            result = new File(result, subprojectName);
        }
        return result.getCanonicalFile();
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

    @Test
    public void testJavaSourcesModel() {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JavaSourcesModel sourcesModel
                        = fetchSingleProjectInfo(connection, JavaSourcesModelBuilder.ONLY_COMPILE);
                assertNotNull("Must have a JavaSourcesModel.", sourcesModel);
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

    @Test
    public void testJarOutputsModel() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                JarOutputsModel jarOutputs
                        = fetchSingleProjectInfo(connection, JarOutputsModelBuilder.INSTANCE);
                assertNotNull("Must have a JarOutputsModel.", jarOutputs);
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

    @Test
    public void testBuiltInModels() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
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
            }
        });
    }
}
