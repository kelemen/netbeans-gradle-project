package org.netbeans.gradle.project.others.test;

import org.netbeans.modules.gsf.testrunner.api.Testcase;

public interface NbGradleTestSuite {
    public Testcase addTestcase(String name);
    public void setStdOut(String stdOut);
    public void setStdErr(String stdErr);

    public void endSuite(long elapsedTimeInMillis);
}
