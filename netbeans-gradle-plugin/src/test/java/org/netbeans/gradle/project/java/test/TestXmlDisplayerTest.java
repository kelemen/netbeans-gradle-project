package org.netbeans.gradle.project.java.test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.api.entry.EmptyProjectTest;
import org.netbeans.gradle.project.api.entry.SampleGradleProject;
import org.netbeans.gradle.project.others.test.NbGradleTestManager;
import org.netbeans.gradle.project.others.test.NbGradleTestSession;
import org.netbeans.gradle.project.others.test.NbGradleTestSuite;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.TestRunnerNodeFactory;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestXmlDisplayerTest {
    private static final String PROJECT_NAME = "empty-project";
    private static final String TEST_NAME = "test";
    private static final String BUILD_DIR = "build";

    private static SampleGradleProject sampleProject;
    private Project rootProject;

    @BeforeClass
    public static void setUpClass() throws IOException {
        sampleProject = EmptyProjectTest.createEmptyProject();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        // To ensure that it can be removed wait until loaded.
        sampleProject.loadProject(PROJECT_NAME).tryWaitForLoadedProject(3, TimeUnit.MINUTES);
        sampleProject.close();
    }

    @Before
    public void setUp() throws IOException {
        Thread.interrupted();

        rootProject = sampleProject.getUnloadedProject(PROJECT_NAME);
        cleanDefaultBuildDirectory(rootProject);
    }

    @After
    public void tearDown() {
    }

    private static File getProjectDirAsFile(Project project) {
        FileObject projectDir = project.getProjectDirectory();
        File result = FileUtil.toFile(projectDir);
        if (result == null) {
            throw new IllegalStateException("Project directory does not exist: " + projectDir);
        }
        return result;
    }

    private static File getBuildDir(Project project) {
        File projectDir = getProjectDirAsFile(project);
        return new File(projectDir, BUILD_DIR);
    }

    private static File getTestResultsDir(Project project) {
        File buildDir = getBuildDir(project);
        return new File(buildDir, "test-results");
    }

    private static File getAndCreateTestResultsDir(Project project) {
        File testResultsDir = getTestResultsDir(project);
        testResultsDir.mkdirs();
        return testResultsDir;
    }

    private static void cleanDefaultBuildDirectory(Project project) throws IOException {
        FileObject buildDir = project.getProjectDirectory().getFileObject(BUILD_DIR);
        if (buildDir != null) {
            buildDir.delete();
        }
    }

    private static String getResourcePath(String relPath) {
        String path = TestXmlDisplayerTest.class.getPackage().getName().replace('.', '/');
        return "/" + path + '/' + relPath;
    }

    private static void initTestResultsDir(Project project, String testResultsZip) throws IOException {
        File testResultsDir = getAndCreateTestResultsDir(project);
        ZipUtils.unzipResource(getResourcePath(testResultsZip), testResultsDir);
    }

    private NbGradleTestSuite mockSuite(final List<? super Testcase> testcases) {
        final TestSession dummySession
                = new TestSession("dummy-session", rootProject, TestSession.SessionType.TEST);

        NbGradleTestSuite testSuite = mock(NbGradleTestSuite.class);
        stub(testSuite.addTestcase(any(String.class))).toAnswer(new Answer<Testcase>() {
            @Override
            public Testcase answer(InvocationOnMock invocation) throws Throwable {
                String name = invocation.getArguments()[0].toString();
                Testcase result = new Testcase(name, null, dummySession);
                testcases.add(result);
                return result;
            }
        });
        return testSuite;
    }

    private static String getExpectedSessionName(Project project) {
        ProjectInformation projectInfo = ProjectUtils.getInformation(project);
        return projectInfo.getDisplayName();
    }

    private static Map<String, Testcase> toTestcaseMap(Collection<Testcase> testcases) {
        Map<String, Testcase> result = CollectionUtils.newHashMap(testcases.size());
        for (Testcase testcase: testcases) {
            if (result.put(testcase.getName(), testcase) != null) {
                fail("Test case has been added multiple times: " + testcase.getName());
            }
        }
        return result;
    }

    private static boolean needsStackTrace(Status status) {
        return status == Status.ERROR || status == Status.FAILED;
    }

    private static Testcase getTestcase(
            Map<String, Testcase> testcases,
            String name,
            Status expectedStatus,
            long expectedTime) {

        Testcase result = testcases.get(name);
        assertNotNull("Must have testcase: " + name, result);
        Status status = result.getStatus();
        assertEquals("Status for " + name, expectedStatus, status);
        assertEquals(expectedTime, result.getTimeMillis());

        if (needsStackTrace(status)) {
            assertNotNull(result.getTrouble());
        }
        else {
            assertNull(result.getTrouble());
        }

        return result;
    }

    private static boolean contains(String[] lines, String pattern) {
        for (String line: lines) {
            if (line.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static void verifyTrouble(Testcase testcase, String expectedLine) {
        String name = testcase.getName();
        Trouble trouble = testcase.getTrouble();
        assertFalse(name + ".error", trouble.isError());
        assertTrue(name + "stackTrace[" + expectedLine + "]", contains(trouble.getStackTrace(), expectedLine));
    }

    @Test
    public void testSingleSessionWithAllKindsOfResults() throws IOException {
        initTestResultsDir(rootProject, "test-results1.zip");

        List<Testcase> testcases = new LinkedList<Testcase>();

        NbGradleTestManager testManager = mock(NbGradleTestManager.class);
        NbGradleTestSession testSession = mock(NbGradleTestSession.class);
        NbGradleTestSuite testSuite = mockSuite(testcases);

        stub(testManager.startSession(
                any(String.class),
                any(Project.class),
                any(TestRunnerNodeFactory.class),
                any(RerunHandler.class))).toReturn(testSession);

        stub(testSession.startTestSuite(any(String.class))).toReturn(testSuite);

        String testClassName = "mypackage.MyIntegTest";
        TestXmlDisplayer testXmlDisplayer = new TestXmlDisplayer(
                rootProject,
                TEST_NAME,
                testManager);

        verifyZeroInteractions(testManager, testSession, testSuite);
        testXmlDisplayer.displayReport(Lookup.EMPTY);

        InOrder inOrder = inOrder(testManager, testSession);
        inOrder.verify(testManager).startSession(
                eq(getExpectedSessionName(rootProject)),
                same(rootProject),
                notNull(TestRunnerNodeFactory.class),
                notNull(RerunHandler.class));
        inOrder.verify(testSession).startTestSuite(eq(testClassName));
        inOrder.verify(testSession).endSession();
        inOrder.verifyNoMoreInteractions();

        verify(testSuite).endSuite(eq(109L));

        Map<String, Testcase> testcaseMap = toTestcaseMap(testcases);
        assertEquals(5, testcaseMap.size());

        Testcase case1 = getTestcase(testcaseMap, "testMyIntegrationFailure1", Status.FAILED, 3);
        assertEquals(testClassName, case1.getClassName());
        verifyTrouble(case1, "mypackage.MyIntegTest.testMyIntegrationFailure1");

        Testcase case2 = getTestcase(testcaseMap, "testMyIntegrationFailure2", Status.FAILED, 0);
        assertEquals(testClassName, case2.getClassName());
        verifyTrouble(case2, "mypackage.MyIntegTest.testMyIntegrationFailure2");

        Testcase case3 = getTestcase(testcaseMap, "testMyIntegrationSuccess1", Status.PASSED, 21);
        assertEquals(testClassName, case3.getClassName());

        Testcase case4 = getTestcase(testcaseMap, "testMyIntegrationSuccess2", Status.PASSED, 0);
        assertEquals(testClassName, case4.getClassName());

        Testcase case5 = getTestcase(testcaseMap, "testSkipped", Status.SKIPPED, 84);
        assertEquals(testClassName, case5.getClassName());
    }
}
