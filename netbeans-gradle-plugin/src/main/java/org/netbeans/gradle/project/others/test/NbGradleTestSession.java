package org.netbeans.gradle.project.others.test;

public interface NbGradleTestSession {
    public NbGradleTestSuite startTestSuite(String suiteName);

    public void endSession();
}
