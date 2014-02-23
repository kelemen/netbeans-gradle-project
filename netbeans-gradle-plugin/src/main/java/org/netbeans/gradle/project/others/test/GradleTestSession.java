package org.netbeans.gradle.project.others.test;

import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.Manager;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

public final class GradleTestSession {
    private final Manager manager;
    private TestSession session;
    private TestSuite suite;

    public GradleTestSession() {
        this.manager = Manager.getInstance();
        this.session = null;
        this.suite = null;
    }

    private boolean hasManager() {
        return manager != null;
    }

    private boolean hasSession() {
        return hasManager() && session != null;
    }

    private boolean hasSuite() {
        return hasSession() && suite != null;
    }

    public void newSession(String name, Project project) {
        newSession(name, project, null);
    }

    public void newSession(String name, Project project, RerunHandler rerunHandler) {
        if (!hasManager()) {
            return;
        }

        session = new TestSession(name, project, TestSession.SessionType.TEST);
        // RerunHandler must be added right after creating the session
        // otherwise it will be ignore by the rerun buttons.
        if (rerunHandler != null) {
            session.setRerunHandler(rerunHandler);
        }
        manager.testStarted(session);
    }

    public void endSession() {
        if (hasSession()) {
            manager.sessionFinished(session);
        }
    }

    public void newSuite(String suiteName) {
        if (!hasSession()) {
            return;
        }

        suite = new TestSuite(suiteName);
        session.addSuite(suite);
        manager.displaySuiteRunning(session, suite);
    }

    public void endSuite(long timeInMillis) {
        if (hasSuite()) {
            Report report = session.getReport(timeInMillis);
            if (report != null) {
                manager.displayReport(session, report, true);
            }
        }
    }

    public Testcase newTestcase(String name) {
        if (!hasSuite()) {
            throw new IllegalStateException("Must call newSession before this method.");
        }

        Testcase testcase = new Testcase(name, null, session);
        session.addTestCase(testcase);
        return testcase;
    }
}
