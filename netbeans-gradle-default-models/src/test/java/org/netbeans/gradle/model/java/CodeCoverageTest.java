package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import org.gradle.tooling.ProjectConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.model.util.ProjectConnectionTask;
import org.netbeans.gradle.model.util.TestUtils;
import org.netbeans.gradle.model.util.ZipUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.model.java.InfoQueries.*;

public final class CodeCoverageTest {
    private static final String ROOT_NAME = "jacoco-project";

    private static File tempFolder = null;
    private static File testedProjectDir = null;

    @BeforeClass
    public static void setUpClass() throws IOException {
        tempFolder = ZipUtils.unzipResourceToTemp(MultiLevelJavaProjectTest.class, "jacoco-project.zip");
        testedProjectDir = new File(tempFolder, ROOT_NAME);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (tempFolder != null) {
            ZipUtils.recursiveDelete(tempFolder);
        }
    }

    private void runTestForSubProject(String projectName, ProjectConnectionTask task) {
        TestUtils.runTestForSubProject(testedProjectDir, projectName, task);
    }

    @Test
    public void testJacoco() {
        runTestForSubProject("", new ProjectConnectionTask() {
            @Override
            public void doTask(ProjectConnection connection) throws Exception {
                JacocoModel jacocoModel
                        = fetchSingleProjectInfo(connection, JavaModelBuilders.JACOCO_BUILDER);
                assertNotNull("JacocoModel", jacocoModel);
            }
        });
    }
}
