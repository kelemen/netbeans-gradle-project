package org.netbeans.gradle.project.others.test;

public final class NbGradleTestManagers {
    private static volatile NbGradleTestManager testManager = new VisualNbGradleTestManager();

    public static NbGradleTestManager getTestManager() {
        return testManager;
    }

    public static void setTestManager(NbGradleTestManager newTestManager) {
        if (newTestManager == null) throw new NullPointerException("newTestManager");

        testManager = newTestManager;
    }

    private NbGradleTestManagers() {
        throw new AssertionError();
    }
}
