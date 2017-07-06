package org.netbeans.gradle.project.tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.jtrim2.cancel.Cancellation;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.entry.SampleProjectRule;
import org.netbeans.gradle.project.properties.standard.CustomVariable;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;
import org.netbeans.gradle.project.tasks.vars.StandardTaskVariable;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;

public class RunGradleTaskTest {
    @ClassRule
    public static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule("run-test-proj.zip");

    @Rule
    public final TestRule timeout = new DisableOnDebug(Timeout.seconds(90));

    @SuppressWarnings("SleepWhileInLoop")
    private static void waitForRun(Path buildDir) throws InterruptedException {
        Path doneFile = buildDir.resolve("done.txt");
        long sleepTime = 5;
        while (!Files.exists(doneFile)) {
            Thread.sleep(sleepTime);
            sleepTime = Math.min(2 * sleepTime, 200);
        }
    }

    private static void runAndWait(NbGradleProject project) throws InterruptedException {
        ActionProvider actionProvider = project.getLookup().lookup(ActionProvider.class);
        actionProvider.invokeAction(ActionProvider.COMMAND_RUN, Lookup.EMPTY);

        waitForRun(buildDir(project));
    }

    private static Path buildDir(NbGradleProject project) {
        return project.getProjectDirectoryAsPath().resolve("build");
    }

    @SuppressWarnings("SleepWhileInLoop")
    private static void deleteDirectoryLenient(Path dir) throws Exception {
        long timeoutMs = 3000;
        long sleepMs = 100;

        int tryCount = (int)(timeoutMs / sleepMs) - 1;
        for (int i = 0; i < tryCount; i++) {
            try {
                NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, dir);
                return;
            } catch (IOException ex) {
            }
            Thread.sleep(sleepMs);
        }

        NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, dir);
    }

    private static NbGradleProject setupRunProject(String... projectPath) throws Throwable {
        NbGradleProject project = PROJECT_REF.loadAndWaitProject(projectPath);
        // FIXME: Currently, there is no way to properly determine if a task has completely
        //        terminated from tests. So, it is possible that a previous test is not
        //        completely terminated and keeps some file open in the build directory.
        //        So, we try a few times to remove the build directory.
        deleteDirectoryLenient(buildDir(project));
        return project;
    }

    private static NbGradleProject testRun(String... projectPath) throws Throwable {
        NbGradleProject project = setupRunProject(projectPath);
        runAndWait(project);
        return project;
    }

    private static void verifyContent(
            NbGradleProject project,
            String outputFileName,
            String expectedContent) throws IOException {

        Path outputFile = buildDir(project).resolve(outputFileName);

        String content = new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8);
        assertEquals("content", expectedContent, content);
    }

    @Test
    public void testManualRun() throws Throwable {
        NbGradleProject project = testRun("run-test-proj", "manual-run");
        verifyContent(project, "test-out.txt", "Hello manual-run");
    }

    @Test
    public void testAutomaticRun() throws Throwable {
        NbGradleProject project = testRun("run-test-proj", "auto-run");
        verifyContent(project, "output-auto.txt", "Auto-Test-Content");
    }

    private static String toQuotedArg(Object arg) {
        return '"' + arg.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + '"';
    }

    @Test
    public void testAutomaticRunWithCustomArgs() throws Throwable {
        NbGradleProject project = setupRunProject("run-test-proj", "auto-run");
        Path buildDir = buildDir(project);

        final String testContent = "NB-TEST-CONTENT-563465";

        Path outputPath = buildDir.resolve("nb-test-output.txt");
        StringBuilder args = new StringBuilder(128);
        args.append(toQuotedArg(outputPath));
        args.append(' ');
        args.append(testContent);

        PropertyReference<CustomVariables> customVariables = project.getCommonProperties().customVariables();
        CustomVariables prevValue = customVariables.getActiveValue();
        try {
            customVariables.setValue(new MemCustomVariables(Arrays.asList(
                    new CustomVariable(StandardTaskVariable.CMD_LINE_ARGS.getVariableName(), args.toString())
            )));

            runAndWait(project);
        } finally {
            customVariables.setValue(prevValue);
        }

        String writtenContent = new String(Files.readAllBytes(outputPath), StandardCharsets.UTF_8);
        assertEquals("content", testContent, writtenContent);
    }

    @Test
    public void testRunSingle() throws Throwable {
        NbGradleProject project = setupRunProject("run-test-proj", "auto-run");

        FileObject rootDir = FileUtil.toFileObject(project.currentModel().getValue().getSettingsDir().toFile());
        FileObject mainClass = rootDir.getFileObject("common-src/testpckg/App2.java");
        DataObject mainClassData = DataObject.find(mainClass);

        ActionProvider actionProvider = project.getLookup().lookup(ActionProvider.class);
        actionProvider.invokeAction(ActionProvider.COMMAND_RUN_SINGLE, Lookups.fixed(mainClassData));

        waitForRun(buildDir(project));

        verifyContent(project, "output-auto.txt", "APP2-Auto-Test-Content");
    }
}
