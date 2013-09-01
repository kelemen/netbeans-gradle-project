package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.ProjectConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleProjectInfoQuery;
import org.netbeans.gradle.model.ProjectInfoBuilder;
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

    private static GenericModelFetcher projectInfoFetcher(ProjectInfoBuilder<?>... builders) {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = new HashMap<Object, GradleProjectInfoQuery<?>>();

        for (int i = 0; i < builders.length; i++) {
            projectInfos.put(i, toQuery(builders[i]));
        }
        return new GenericModelFetcher(buildInfos, projectInfos);
    }

    private void runTestForSubProject(String projectName, ProjectConnectionTask task) {
        String relName = projectName.replace(":", File.separator);
        File subDir = new File(testedProjectDir, relName);
        runTestsForProject(subDir, task);
    }

    @Test
    public void testJavaSourcesModel() {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                GenericModelFetcher modelFetcher = projectInfoFetcher(JavaSourcesModelBuilder.INSTANCE);
                FetchedModels models = modelFetcher.getModels(connection, defaultInit());

                assertTrue(models.getBuildInfoResults().isEmpty());

                JavaSourcesModel sourcesModel = (JavaSourcesModel)models
                        .getDefaultProjectModels()
                        .getProjectInfoResults()
                        .get(0);
                assertNotNull("Must have a JavaSourcesModel.", sourcesModel);
            }
        });
    }

    @Test
    public void testJavaCompatibilityModel() throws IOException {
        runTestForSubProject("apps:app1", new ProjectConnectionTask() {
            public void doTask(ProjectConnection connection) throws Exception {
                GenericModelFetcher modelFetcher = projectInfoFetcher(JavaCompatibilityModelBuilder.INSTANCE);
                FetchedModels models = modelFetcher.getModels(connection, defaultInit());

                assertTrue(models.getBuildInfoResults().isEmpty());

                JavaCompatibilityModel sourcesModel = (JavaCompatibilityModel)models
                        .getDefaultProjectModels()
                        .getProjectInfoResults()
                        .get(0);
                assertNotNull("Must have a JavaCompatibilityModel.", sourcesModel);
            }
        });
    }
}
