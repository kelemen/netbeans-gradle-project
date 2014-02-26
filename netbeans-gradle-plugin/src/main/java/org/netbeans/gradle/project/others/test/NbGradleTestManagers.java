package org.netbeans.gradle.project.others.test;

public final class NbGradleTestManagers {
    private static final NbGradleTestManager testManager = new VisualNbGradleTestManager();

    public static NbGradleTestManager getTestManager() {
        return testManager;
    }

    private NbGradleTestManagers() {
        throw new AssertionError();
    }
}
