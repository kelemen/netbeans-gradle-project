package org.netbeans.gradle.project.api.task;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;

/**
 * Defines the context where the associated daemon task is to be executed.
 * A daemon task can be a Gradle command to be executed or a request to
 * load models built from the build scripts of a project.
 * <P>
 * Instances of this class are immutable and therefore safe to be
 * shared by multiple concurrent threads.
 */
public final class DaemonTaskContext {
    private final Project project;
    private final boolean modelLoading;

    /**
     * Creates a new context object with the given arguments.
     *
     * @param project the project for which the daemon task is to be
     *   executed. This argument cannot be {@code null}.
     * @param modelLoading {@code true} if the associated daemon task is a
     *   request to load models from the build scripts of the
     *   project rather than executing a Gradle command
     */
    public DaemonTaskContext(@Nonnull Project project, boolean modelLoading) {
        this.project = Objects.requireNonNull(project, "project");
        this.modelLoading = modelLoading;
    }

    /**
     * Returns the project object for which the daemon task is to
     * be executed.
     *
     * @return the project object for which the daemon task is to
     *   be executed. This method never returns {@code null}.
     */
    @Nonnull
    public Project getProject() {
        return project;
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
