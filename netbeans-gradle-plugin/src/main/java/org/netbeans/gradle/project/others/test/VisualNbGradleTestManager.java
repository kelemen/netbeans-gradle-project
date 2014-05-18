package org.netbeans.gradle.project.others.test;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.Manager;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.TestRunnerNodeFactory;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

public final class VisualNbGradleTestManager implements NbGradleTestManager {
    private final Manager manager;

    public VisualNbGradleTestManager() {
        this.manager = Manager.getInstance();
    }

    @Override
    public NbGradleTestSession startSession(
            String name,
            Project project,
            TestRunnerNodeFactory nodeFactory,
            RerunHandler rerunHandler) {

        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(project, "project");

        TestSession session = nodeFactory != null
                ? new TestSession(name, project, TestSession.SessionType.TEST, nodeFactory)
                : new TestSession(name, project, TestSession.SessionType.TEST);
        // RerunHandler must be added right after creating the session
        // otherwise it will be ignore by the rerun buttons.
        if (rerunHandler != null) {
            session.setRerunHandler(rerunHandler);
        }
        manager.testStarted(session);

        return new NbGradleTestSessionImpl(manager, session);
    }

    private static final class NbGradleTestSessionImpl implements NbGradleTestSession {
        private final Manager manager;
        private final TestSession session;

        public NbGradleTestSessionImpl(Manager manager, TestSession session) {
            assert manager != null;
            assert session != null;

            this.manager = manager;
            this.session = session;
        }

        @Override
        public NbGradleTestSuite startTestSuite(String suiteName) {
            ExceptionHelper.checkNotNullArgument(suiteName, "suiteName");

            final TestSuite suite = new TestSuite(suiteName);
            session.addSuite(suite);
            manager.displaySuiteRunning(session, suite);

            return new NbGradleTestSuiteImpl(manager, session);
        }

        @Override
        public void endSession() {
            manager.sessionFinished(session);
        }
    }

    private static final class NbGradleTestSuiteImpl implements NbGradleTestSuite {
        private final Manager manager;
        private final TestSession session;
        private String stdOut;
        private String stdErr;

        public NbGradleTestSuiteImpl(Manager manager, TestSession session) {
            assert manager != null;
            assert session != null;

            this.manager = manager;
            this.session = session;

            this.stdErr = null;
            this.stdOut = null;
        }

        @Override
        public Testcase addTestcase(String name) {
            Testcase testcase = new Testcase(name, null, session);
            session.addTestCase(testcase);
            return testcase;
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
        public void endSuite(long elapsedTimeInMillis) {
            Report report = session.getReport(elapsedTimeInMillis);
            if (report != null) {
                if (stdErr != null) {
                    manager.displayOutput(session, stdErr, true);
                }
                if (stdOut != null) {
                    manager.displayOutput(session, stdOut, false);
                }
                manager.displayReport(session, report, true);
            }
        }
    }
}
