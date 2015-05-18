package org.netbeans.gradle.project.api.task;

public final class DaemonTaskContext {
    private final boolean modelLoading;

    public DaemonTaskContext(boolean modelLoading) {
        this.modelLoading = modelLoading;
    }

    public boolean isModelLoading() {
        return modelLoading;
    }
}
