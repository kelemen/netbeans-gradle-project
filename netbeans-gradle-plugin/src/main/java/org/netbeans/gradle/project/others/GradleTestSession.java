package org.netbeans.gradle.project.others;

import org.netbeans.api.project.Project;

import static org.netbeans.gradle.project.others.ReflectionHelper.*;

public final class GradleTestSession {
    private static final PluginClassFactory CLASS_FACTORY
            = new PluginClassFactory("org.netbeans.modules.gsf.testrunner");

    private static final PluginClass MANAGER
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.Manager");
    private static final PluginClass TEST_SESSION
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.TestSession");
    private static final PluginEnum SESSION_TYPE
            = new PluginEnum(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.TestSession$SessionType");
    private static final PluginClass TEST_SUITE
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.TestSuite");
    private static final PluginClass TESTCASE
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.Testcase");
    private static final PluginEnum STATUS
            = new PluginEnum(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.Status");
    private static final PluginClass TROUBLE
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.Trouble");
    private static final PluginClass REPORT
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.Report");
    private static final PluginClass RERUN_HANDLER
            = new PluginClass(CLASS_FACTORY, "org.netbeans.modules.gsf.testrunner.api.RerunHandler");

    private static final PluginClassMethod MANAGER_GET_INSTANCE
            = PluginClassMethod.noArgMethod(MANAGER, "getInstance");
    private static final PluginClassMethod MANAGER_TEST_STARTED
            = new PluginClassMethod(MANAGER, "testStarted", TEST_SESSION);
    private static final PluginClassMethod MANAGER_DISPLAY_SUITE_RUNNING
            = new PluginClassMethod(MANAGER, "displaySuiteRunning", TEST_SESSION, TEST_SUITE);
    private static final PluginClassMethod MANAGER_DISPLAY_REPORT
            = new PluginClassMethod(MANAGER, "displayReport", TEST_SESSION, REPORT, constClassFinder(boolean.class));
    private static final PluginClassMethod MANAGER_SESSION_FINISHED
            = new PluginClassMethod(MANAGER, "sessionFinished", TEST_SESSION);
    private static final PluginClassMethod TEST_SESSION_ADD_SUITE
            = new PluginClassMethod(TEST_SESSION, "addSuite", TEST_SUITE);
    private static final PluginClassMethod TEST_SESSION_GET_REPORT
            = new PluginClassMethod(TEST_SESSION, "getReport", long.class);
    private static final PluginClassMethod TEST_SESSION_ADD_TESTCASE
            = new PluginClassMethod(TEST_SESSION, "addTestCase", TESTCASE);
    private static final PluginClassMethod TESTCASE_SET_STATUS
            = new PluginClassMethod(TESTCASE, "setStatus", STATUS);
    private static final PluginClassMethod TESTCASE_SET_CLASSNAME
            = new PluginClassMethod(TESTCASE, "setClassName", String.class);
    private static final PluginClassMethod TESTCASE_SET_TIMEMILLIS
            = new PluginClassMethod(TESTCASE, "setTimeMillis", long.class);
    private static final PluginClassMethod TESTCASE_SET_TROUBLE
            = new PluginClassMethod(TESTCASE, "setTrouble", TROUBLE);
    private static final PluginClassMethod TROUBLE_SET_STACKTRACE
            = new PluginClassMethod(TROUBLE, "setStackTrace", String[].class);

    private static final PluginClassConstructor CONSTR_TEST_SESSION = new PluginClassConstructor(
            TEST_SESSION,
            constClassFinder(String.class),
            constClassFinder(Project.class),
            SESSION_TYPE);

    private static final PluginClassConstructor CONSTR_TEST_SUITE = new PluginClassConstructor(
            TEST_SUITE,
            String.class);

    private static final PluginClassConstructor CONSTR_TESTCASE = new PluginClassConstructor(
            TESTCASE,
            constClassFinder(String.class),
            constClassFinder(String.class),
            TEST_SESSION);

    private static final PluginClassConstructor CONSTR_TROUBLE = new PluginClassConstructor(
            TROUBLE,
            boolean.class);

    private final Object manager;
    private Object session;
    private Object suite;

    public GradleTestSession() {
        this.manager = MANAGER_GET_INSTANCE.tryInvoke(null);
        this.session = null;
    }

    private static Object tryCreateNewSession(String name, Project project) {
        Object sessionType = SESSION_TYPE.tryGetEnumConst("TEST");
        if (sessionType == null) {
            return null;
        }

        return CONSTR_TEST_SESSION.tryCreateInstance(name, project, sessionType);
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
        if (!hasManager()) {
            return;
        }

        session = tryCreateNewSession(name, project);

        if (hasSession()) {
            MANAGER_TEST_STARTED.tryInvoke(manager, session);
        }
    }

    public void endSession() {
        if (hasSession()) {
            MANAGER_SESSION_FINISHED.tryInvoke(manager, session);
        }
    }

    public void newSuite(String suiteName) {
        if (!hasSession()) {
            return;
        }

        suite = CONSTR_TEST_SUITE.tryCreateInstance(suiteName);
        TEST_SESSION_ADD_SUITE.tryInvoke(session, suite);
        MANAGER_DISPLAY_SUITE_RUNNING.tryInvoke(manager, session, suite);
    }

    public void endSuite(long timeInMillis) {
        if (hasSuite()) {
            TEST_SESSION_GET_REPORT.tryInvoke(session, timeInMillis);
            Object report = TEST_SESSION_GET_REPORT.tryInvoke(session, timeInMillis);
            if (report != null) {
                MANAGER_DISPLAY_REPORT.tryInvoke(manager, session, report, true);
            }
        }
    }

    public Testcase newTestcase(String name) {
        if (!hasSuite()) {
            return new Testcase(null);
        }

        return Testcase.create(session, name);
    }

    public enum Status {
        PASSED,
        PENDING,
        FAILED,
        ERROR,
        ABORTED,
        SKIPPED,
        PASSEDWITHERRORS,
        IGNORED;
    }

    public static final class Testcase {
        private final Object testcase;

        private Testcase(Object testcase) {
            this.testcase = testcase;
        }

        private static Testcase create(Object session, String name) {
            Object testcase = CONSTR_TESTCASE.tryCreateInstance(name, null, session);
            Testcase result = new Testcase(testcase);

            if (testcase != null && session != null) {
                TEST_SESSION_ADD_TESTCASE.tryInvoke(session, testcase);
            }

            return result;
        }

        public void setClassName(String name) {
            if (testcase != null) {
                TESTCASE_SET_CLASSNAME.tryInvoke(testcase, name);
            }
        }

        public void setStatus(Status status) {
            if (testcase != null) {
                Object statusValue = STATUS.tryGetEnumConst(status.name());
                if (statusValue != null) {
                    TESTCASE_SET_STATUS.tryInvoke(testcase, statusValue);
                }
            }
        }

        public void setTimeMillis(long timeMillis) {
            if (testcase != null) {
                TESTCASE_SET_TIMEMILLIS.tryInvoke(testcase, timeMillis);
            }
        }

        public void setTrouble(boolean error, String[] stackTrace) {
            if (testcase != null) {
                Object trouble = CONSTR_TROUBLE.tryCreateInstance(error);
                if (trouble != null) {
                    Object stackTraceCopy = stackTrace != null ? stackTrace.clone() : null;
                    TROUBLE_SET_STACKTRACE.tryInvoke(trouble, stackTraceCopy);
                    TESTCASE_SET_TROUBLE.tryInvoke(testcase, trouble);
                }
            }
        }
    }
}
