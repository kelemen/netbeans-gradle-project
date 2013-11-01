package org.netbeans.gradle.model.util;

import java.io.File;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;

public final class TestUtils {
    private static final String MAIN_GRADLE_VERSION = "1.8";

    private static void runTestForProject(
            String gradleVersion,
            File projectDir,
            ProjectConnectionTask task) throws Exception {

        if (projectDir == null) throw new NullPointerException("projectDir");
        if (task == null) throw new NullPointerException("task");

        GradleConnector connector = GradleConnector.newConnector();
        connector.useGradleVersion(gradleVersion);
        connector.forProjectDirectory(projectDir);

        ProjectConnection connection = connector.connect();
        try {
            task.doTask(connection);
        } finally {
            connection.close();
        }
    }

    public static void runTestsForProject(File projectDir, ProjectConnectionTask task) {
        try {
            // TODO: Run tests for other versions as well.
            runTestForProject(MAIN_GRADLE_VERSION, projectDir, task);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static File getJDKHome() {
        File jreHome = new File(System.getProperty("java.home"));
        return jreHome.getParentFile();
    }

    public static OperationInitializer defaultInit() {
        return DefaultOperationInit.INSTANCE;
    }

    private TestUtils() {
        throw new AssertionError();
    }

    private enum DefaultOperationInit implements OperationInitializer {
        INSTANCE;

        public void initOperation(BuildOperationArgs args) {
            args.setJvmArguments(new String[]{"-Xmx512m"});
            args.setJavaHome(getJDKHome());
            args.setStandardOutput(System.out);
        }
    }
}
