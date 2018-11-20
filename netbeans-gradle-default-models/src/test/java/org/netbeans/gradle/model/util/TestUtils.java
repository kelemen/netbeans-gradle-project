package org.netbeans.gradle.model.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.util.GradleVersion;
import org.junit.Assume;
import org.netbeans.gradle.model.BuildOperationArgs;
import org.netbeans.gradle.model.OperationInitializer;

public final class TestUtils {
    private static final AtomicReference<String> TESTED_GRADLE_VERSION_REF = new AtomicReference<String>(null);

    public static boolean isGradleAtLeast(String version) {
        String currentVersionStr = getTestedGradleVersion();
        GradleVersion currentVersion = "".equals(currentVersionStr)
                ? GradleVersion.current()
                : GradleVersion.version(currentVersionStr);
        GradleVersion cmpVersion = GradleVersion.version(version);

        return currentVersion.compareTo(cmpVersion) >= 0;
    }

    private static String normString(String value) {
        if (value == null) {
            return null;
        }

        String result = value.trim();
        if (result.length() == 0) {
            return null;
        }
        return result;
    }

    private static String getTestedGradleVersion() {
        String result = TESTED_GRADLE_VERSION_REF.get();
        if (result == null) {
            result = normString(System.getProperty("TESTED_GRADLE_DAEMON_VERSION"));
            if (result == null) {
                String skipTests = normString(System.getProperty("SKIP_GRADLE_DAEMON_TESTS"));
                if ("true".equalsIgnoreCase(skipTests)) {
                    result = "";
                }
                else {
                    result = GradleVersion.current().getVersion();
                }
            }

            TESTED_GRADLE_VERSION_REF.compareAndSet(null, result);
            result = TESTED_GRADLE_VERSION_REF.get();
        }
        return result;
    }

    private static void runTestForProject(
            String gradleVersion,
            File projectDir,
            ProjectConnectionTask task) throws Exception {

        if (projectDir == null) throw new NullPointerException("projectDir");
        if (task == null) throw new NullPointerException("task");

        GradleConnector connector = GradleConnector.newConnector();
        connector.useGradleVersion(gradleVersion);
        connector.forProjectDirectory(projectDir);

        if (connector instanceof DefaultGradleConnector) {
            ((DefaultGradleConnector)connector).daemonMaxIdleTime(60, TimeUnit.SECONDS);
        }

        ProjectConnection connection = connector.connect();
        try {
            task.doTask(connection);
        } finally {
            connection.close();
        }
    }

    private static void assumeHasTestedGradle() {
        Assume.assumeTrue(getTestedGradleVersion().length() != 0);
    }

    public static void runTestsForProject(File projectDir, ProjectConnectionTask task) {
        assumeHasTestedGradle();

        try {
            runTestForProject(getTestedGradleVersion(), projectDir, task);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static void runTestForSubProject(
            File projectDir,
            String relativeProjectPath,
            ProjectConnectionTask task) {
        assumeHasTestedGradle();

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
        return BasicFileUtils.getSubPath(root, subPaths).getCanonicalFile();
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

        @Override
        public void initOperation(BuildOperationArgs args) {
            args.setJvmArguments(new String[]{"-Xmx512m"});
            args.setJavaHome(getJDKHome());
            args.setStandardOutput(System.out);
        }
    }
}
