package org.netbeans.gradle.project.others.test;

import org.netbeans.modules.gsf.testrunner.api.Testcase;

public interface NbGradleTestSuite {
    public Testcase addTestcase(String name);
    public void endSuite(long elapsedTimeInMillis);
}
