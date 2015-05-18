package org.netbeans.gradle.project.api.task;

/**
 * Defines the context where the associated daemon task is to be executed.
 * A daemon task can be a Gradle command to be executed or a request to
 * load models built from the build scripts of a project.
 * <P>
 * Instances of this class are immutable and therefore safe to be
 * shared by multiple concurrent threads.
 */
public final class DaemonTaskContext {
    private final boolean modelLoading;

    /**
     * Creates a new context object with the given arguments.
     *
     * @param modelLoading {@code true} if the associated daemon task is a
     *   request to load models from the build scripts of the
     *   project rather than executing a Gradle command
     */
    public DaemonTaskContext(boolean modelLoading) {
        this.modelLoading = modelLoading;
    }

    /**
     * Returns {@code true} if the associated daemon task is a
     * request to load models from the build scripts of the
     * project rather than executing a Gradle command.
     *
     * @return {@code true} if the associated daemon task is a
     *   request to load models from the build scripts of the
     *   project, {@code false} otherwise
     */
    public boolean isModelLoading() {
        return modelLoading;
    }
}
