package org.netbeans.gradle.project.others.test;

import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.Manager;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

public final class NbGradleTestManager {
    private final Manager manager;

    public NbGradleTestManager() {
        this.manager = Manager.getInstance();
    }

    public NbGradleTestSession startSession(String name, Project project, RerunHandler rerunHandler) {
        if (name == null) throw new NullPointerException("name");
        if (project == null) throw new NullPointerException("project");

        TestSession session = new TestSession(name, project, TestSession.SessionType.TEST);
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
            if (suiteName == null) throw new NullPointerException("suiteName");

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

        public NbGradleTestSuiteImpl(Manager manager, TestSession session) {
            assert manager != null;
            assert session != null;

            this.manager = manager;
            this.session = session;
        }

        @Override
        public Testcase addTestcase(String name) {
            Testcase testcase = new Testcase(name, null, session);
            session.addTestCase(testcase);
            return testcase;
        }

        @Override
        public void endSuite(long elapsedTimeInMillis) {
            Report report = session.getReport(elapsedTimeInMillis);
            if (report != null) {
                manager.displayReport(session, report, true);
            }
        }
    }
}
