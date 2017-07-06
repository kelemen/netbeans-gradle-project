package org.netbeans.gradle.project.api.task;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;
import org.openide.windows.InputOutput;

/**
 * Defines the execution context of a started Gradle command. The context is
 * captured right before task execution but before any
 * {@link GradleCommandServiceFactory command service} has been started.
 *
 * @see GradleCommandServiceFactory
 */
public final class GradleCommandContext {
    private final Project project;
    private final InputOutput outputTab;

    /**
     * Creates a new {@code GradleCommandContext} with the given properties.
     *
     * @param project the project associated with the command to be executed.
     *   This argument cannot be {@code null}.
     * @param outputTab the input and output tab of the associated command.
     *   This argument cannot be {@code null}.
     */
    public GradleCommandContext(@Nonnull Project project, @Nonnull InputOutput outputTab) {
        this.project = Objects.requireNonNull(project, "project");
        this.outputTab = Objects.requireNonNull(outputTab, "outputTab");
    }

    /**
     * Returns the project associated with the command to be executed.
     *
     * @return the project associated with the command to be executed. This
     *   method may never return {@code null}.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns the input and output tab of the associated command. The return
     * value might be used to write to the output window of the associated
     * Gradle command.
     *
     * @return the input and output tab of the associated command. This method
     *   may never be {@code null}.
     */
    public InputOutput getOutputTab() {
        return outputTab;
    }
}
