package org.netbeans.gradle.model.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;

public final class TestUtils {
    private static final String MAIN_GRADLE_VERSION = "1.9";

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

    public static void runTestForSubProject(
            File projectDir,
            String relativeProjectPath,
            ProjectConnectionTask task) {

        try {
            File subDir;
            if (relativeProjectPath.length() > 0) {
                String relName = relativeProjectPath.replace(":", File.separator);
                subDir = new File(projectDir, relName);
            }
            else {
                subDir = projectDir;
            }

            runTestsForProject(subDir, task);
        } catch (Throwable ex) {
            AssertionError error = new AssertionError("Failure for project \":" + relativeProjectPath + "\": "
                    + ex.getMessage());
            error.initCause(ex);
            throw error;
        }
    }

    private static String makeRelative(String projectPath) {
        if (projectPath.length() == 0) {
            return projectPath;
        }

        int firstNonColonIndex = 0;
        for (int i = 0; i < projectPath.length(); i++) {
            if (projectPath.charAt(i) != ':') {
                firstNonColonIndex = i;
                break;
            }
        }

        return projectPath.substring(firstNonColonIndex);
    }

    public static File getSubProjectDir(File root, String projectPath) throws IOException {
        String relativeProjectPath = makeRelative(projectPath);
        if (relativeProjectPath.length() == 0) {
            return root;
        }

        String[] pathElements = relativeProjectPath.split(Pattern.quote(":"));
        return getSubPath(root, pathElements);
    }

    public static File getSubPath(File root, String... subPaths) throws IOException {
        File result = root;
        for (String subprojectName: subPaths) {
            result = new File(result, subprojectName);
        }
        return result.getCanonicalFile();
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
