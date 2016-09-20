package org.netbeans.gradle.project.java.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.api.entry.EmptyProjectTest;
import org.netbeans.gradle.project.api.entry.SampleProjectRule;
import org.netbeans.gradle.project.others.test.NbGradleTestManager;
import org.netbeans.gradle.project.others.test.NbGradleTestSession;
import org.netbeans.gradle.project.others.test.NbGradleTestSuite;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.modules.gsf.testrunner.ui.api.TestRunnerNodeFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

import static org.junit.Assert.*;

public class TestXmlDisplayerTest {
    @ClassRule
    public static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule(EmptyProjectTest.EMPTY_PROJECT_RESOURCE);

    private static final String TEST_NAME = "test";
    private static final String BUILD_DIR = "build";

    private Project rootProject;

    @Before
    public void setUp() throws IOException {
        Thread.interrupted();

        rootProject = PROJECT_REF.getUnloadedProject(EmptyProjectTest.EMPTY_PROJECT_NAME);
        cleanDefaultBuildDirectory(rootProject);
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

    private static String getExpectedSessionName(Project project) {
        ProjectInformation projectInfo = ProjectUtils.getInformation(project);
        return projectInfo.getDisplayName();
    }

    private static boolean contains(String[] lines, String pattern) {
        for (String line: lines) {
            if (line.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testSingleSessionWithAllKindsOfResults() throws IOException {
        initTestResultsDir(rootProject, "test-results1.zip");

        ExpectedSession session1 = new ExpectedSession(rootProject);
        ExpectedSuite suite1 = session1.addSuite("mypackage.MyIntegTest", 109, "HELLO1\nHELLO2\n", "");

        suite1.addFailed("testMyIntegrationFailure1", 3);
        suite1.addFailed("testMyIntegrationFailure2", 0);
        suite1.addPassed("testMyIntegrationSuccess1", 21);
        suite1.addPassed("testMyIntegrationSuccess2", 0);
        suite1.addSkipped("testSkipped", 84);

        MockManager mockManager = new MockManager();
        TestXmlDisplayer testXmlDisplayer = new TestXmlDisplayer(
                rootProject,
                TEST_NAME,
                mockManager);

        testXmlDisplayer.displayReport(Lookup.EMPTY);

        mockManager.verifySessions(session1);
    }

    private static final class MockManager extends ErrorCollector implements NbGradleTestManager {
        private final Collection<MockSession> sessions;

        public MockManager() {
            super("MockManager");
            this.sessions = new ConcurrentLinkedQueue<>();
        }

        @Override
        public NbGradleTestSession startSession(
                String name,
                Project project,
                TestRunnerNodeFactory nodeFactory,
                RerunHandler rerunHandler) {

            MockSession session = new MockSession(name, project);
            sessions.add(session);

            return session;
        }

        public void verifySessions(ExpectedSession... expectedSessions) {
            verifyNoErrors();

            MockSession[] actualSessions = sessions.toArray(new MockSession[0]);

            int length = Math.min(actualSessions.length, expectedSessions.length);
            for (int i = 0; i < length; i++) {
                actualSessions[i].verifySession(expectedSessions[i]);
            }

            assertEquals("Session count", expectedSessions.length, actualSessions.length);
        }
    }

    private static final class MockSession extends ErrorCollector implements NbGradleTestSession {
        public final String name;
        public final Project project;
        private final TestSession gsfSession;

        private final Collection<MockSuite> suites;
        private final AtomicBoolean closed;

        public MockSession(String name, Project project) {
            super("MockSession." + name);

            this.name = name;
            this.project = project;
            this.gsfSession = new TestSession(name, project, TestSession.SessionType.TEST);
            this.suites = new ConcurrentLinkedQueue<>();
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public NbGradleTestSuite startTestSuite(String suiteName) {
            MockSuite suite = new MockSuite(gsfSession, suiteName);
            suites.add(suite);

            return suite;
        }

        @Override
        public void endSession() {
            if (closed.getAndSet(true)) {
                addStateError("endSession called multiple times.");
            }
            else {
                for (MockSuite suite: suites) {
                    suite.mustHaveBeenClosed("parent session has been closed");
                }
            }
        }

        public void mustHaveBeenClosed(String reason) {
            if (!closed.get()) {
                addStateError("This session must have been closed because "+ reason);
            }
        }

        private void verifySession(ExpectedSession expectedSession) {
            assertNotNull("Expected session with name " + name, expectedSession);

            verifyNoErrors();

            assertEquals(expectedSession.sessionName, name);
            assertSame(expectedSession.project, project);

            for (MockSuite suite: suites) {
                ExpectedSuite expectedSuite = expectedSession.suites.get(suite.suiteName);
                suite.verifySuite(expectedSuite);
            }

            assertEquals("Suite count", expectedSession.suites.size(), suites.size());
            assertTrue("closed", closed.get());
        }
    }

    private static final class MockSuite extends ErrorCollector implements NbGradleTestSuite {
        private final TestSession parent;
        private final String suiteName;
        private final AtomicBoolean closed;
        private String stdOut;
        private String stdErr;

        private final Collection<Testcase> testcases;
        private volatile long elapsedTimeInMillis;

        public MockSuite(TestSession parent, String suiteName) {
            super("MockSuite." + suiteName);

            this.parent = parent;
            this.suiteName = suiteName;
            this.closed = new AtomicBoolean(false);
            this.testcases = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void setStdOut(String stdOut) {
            this.stdOut = stdOut;
        }

        @Override
        public void setStdErr(String stdErr) {
            this.stdErr = stdErr;
        }

        @Override
        public Testcase addTestcase(String name) {
            Testcase testcase = new Testcase(name, null, parent);
            testcases.add(testcase);
            return testcase;
        }

        @Override
        public void endSuite(long elapsedTimeInMillis) {
            if (closed.getAndSet(true)) {
                addStateError("endSuite called multiple times.");
            }
            else {
                this.elapsedTimeInMillis = elapsedTimeInMillis;
            }
        }

        public void mustHaveBeenClosed(String reason) {
            if (!closed.get()) {
                addStateError("This suite must have been closed because "+ reason);
            }
        }

        private static List<String> safeToLines(String text) {
            return text != null ? StringUtils.toLines(text) : Collections.<String>emptyList();
        }

        private void verifySuite(ExpectedSuite expectedSuite) {
            assertNotNull("Expected suite with name " + suiteName, expectedSuite);

            verifyNoErrors();

            assertEquals(expectedSuite.suiteName, suiteName);

            for (Testcase testcase: testcases) {
                String testcaseName = testcase.getName();
                TestcaseVerifier verifier = expectedSuite.verifiers.get(testcaseName);
                assertNotNull("Expected test case " + testcaseName, verifier);
                verifier.verifyTestCase(testcase);
            }

            assertEquals("suiteTimeMillis", expectedSuite.suiteTimeMillis, elapsedTimeInMillis);
            assertEquals("Testcase count", expectedSuite.verifiers.size(), testcases.size());
            assertTrue("closed", closed.get());

            assertEquals("stdout", safeToLines(expectedSuite.stdOut), safeToLines(stdOut));
            assertEquals("stderr", safeToLines(expectedSuite.stdErr), safeToLines(stdErr));
        }
    }

    private static abstract class ErrorCollector {
        private final String name;
        private final Collection<Throwable> errors;

        public ErrorCollector(String name) {
            this.name = name;
            this.errors = new ConcurrentLinkedQueue<>();
        }

        public final void addError(Throwable error) {
            errors.add(error);
        }

        protected final String errorMessage(String message) {
            return name + ": " + message;
        }

        public final void addArgumentError(String message) {
            addError(new IllegalArgumentException(errorMessage(message)));
        }

        public final void addStateError(String message) {
            addError(new IllegalStateException(errorMessage(message)));
        }

        public final void verifyNoErrors() {
            List<Throwable> currentErrors = new ArrayList<>(errors);
            if (currentErrors.isEmpty()) {
                return;
            }

            Throwable toThrow = currentErrors.get(0);
            for (Throwable ex: currentErrors) {
                ex.printStackTrace(System.err);
            }

            throw new AssertionError(toThrow);
        }
    }

    private static final class ExpectedSession {
        private final String sessionName;
        private final Project project;

        private final Map<String, ExpectedSuite> suites;

        public ExpectedSession(Project project) {
            this.project = project;
            this.sessionName = getExpectedSessionName(project);
            this.suites = new HashMap<>();
        }

        public ExpectedSuite addSuite(String suiteName, long suiteTimeMillis, String stdOut, String stdErr) {
            ExpectedSuite suite = new ExpectedSuite(suiteName, suiteTimeMillis, stdOut, stdErr);
            suites.put(suite.getSuiteName(), suite);

            return suite;
        }
    }

    private static final class ExpectedSuite {
        private final String suiteName;
        private final long suiteTimeMillis;
        private final Map<String, TestcaseVerifier> verifiers;

        private final String stdOut;
        private final String stdErr;

        public ExpectedSuite(String suiteName, long suiteTimeMillis, String stdOut, String stdErr) {
            this.suiteName = suiteName;
            this.verifiers = new HashMap<>();
            this.suiteTimeMillis = suiteTimeMillis;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }

        public String getSuiteName() {
            return suiteName;
        }

        private <T extends AbstractTestcaseVerifier> T returnVerifier(
                T verifier,
                Status status,
                long timeMillis) {

            verifier.status = status;
            verifier.timeMillis = timeMillis;

            verifiers.put(verifier.getName(), verifier);
            return verifier;
        }

        public NotFailedTestCaseVerifier addSkipped(String name, long timeMillis) {
            NotFailedTestCaseVerifier verifier = new NotFailedTestCaseVerifier(name, suiteName);
            return returnVerifier(verifier, Status.SKIPPED, timeMillis);
        }

        public NotFailedTestCaseVerifier addPassed(String name, long timeMillis) {
            NotFailedTestCaseVerifier verifier = new NotFailedTestCaseVerifier(name, suiteName);
            return returnVerifier(verifier, Status.PASSED, timeMillis);
        }

        public FailedTestCaseVerifier addFailed(String name, long timeMillis) {
            FailedTestCaseVerifier verifier = new FailedTestCaseVerifier(name, suiteName);
            return returnVerifier(verifier, Status.FAILED, timeMillis);
        }
    }

    private static final class NotFailedTestCaseVerifier extends AbstractTestcaseVerifier {
        public NotFailedTestCaseVerifier(String name, String testClassName) {
            super(name, testClassName);
        }

        @Override
        public void verifyTestCase(Testcase testcase) {
            super.verifyTestCase(testcase);

            assertNull(message("Trouble"), testcase.getTrouble());
        }

    }

    private static final class FailedTestCaseVerifier extends AbstractTestcaseVerifier {
        public boolean error = false;
        public String expectedStackTraceLine = testClassName + "." + getName();

        public FailedTestCaseVerifier(String name, String testClassName) {
            super(name, testClassName);
        }

        @Override
        public void verifyTestCase(Testcase testcase) {
            super.verifyTestCase(testcase);

            Trouble trouble = testcase.getTrouble();
            assertNotNull(message("Trouble"), trouble);

            assertEquals(message("Trouble.isError"), error, trouble.isError());
            assertTrue(message("StackTrace[" + expectedStackTraceLine + "]"),
                    contains(trouble.getStackTrace(), expectedStackTraceLine));
        }
    }

    private static abstract class AbstractTestcaseVerifier implements TestcaseVerifier {
        public final String name;
        public final String testClassName;

        public Status status;
        public long timeMillis;

        public AbstractTestcaseVerifier(String name, String testClassName) {
            this.name = name;
            this.testClassName = testClassName;
        }

        @Override
        public final String getName() {
            return name;
        }

        protected final String message(String property) {
            return property + " of " + name;
        }

        @Override
        public void verifyTestCase(Testcase testcase) {
            assertEquals(message("Name"), name, testcase.getName());
            assertEquals(message("ClassName"), testClassName, testcase.getClassName());
            assertEquals(message("Status"), status, testcase.getStatus());
            assertEquals(message("TimeMillis"), timeMillis, testcase.getTimeMillis());
        }
    }

    private static interface TestcaseVerifier {
        public String getName();
        public void verifyTestCase(Testcase testcase);
    }
}
