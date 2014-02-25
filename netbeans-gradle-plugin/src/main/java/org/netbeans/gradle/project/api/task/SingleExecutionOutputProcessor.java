package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;

/**
 * Defines factory of {@link TaskOutputProcessor} processing the standard output
 * of the associated Gradle command. The factory is asked to create a new
 * {@code TaskOutputProcessor} for each execution of the command.
 *
 * @see CustomCommandActions
 */
public interface SingleExecutionOutputProcessor {
    /**
     * Creates a {@code TaskOutputProcessor} for a single Gradle command
     * execution.
     * <P>
     * This method is called from the same thread where the command is to be
     * executed. Therefore it is forbidden for this method to wait for the
     * completion of a particular Gradle command as this could likely result in
     * a dead-lock.
     *
     * @param project the project against which the command is to be executed.
     *   This argument cannot be {@code null}.
     * @return the {@code TaskOutputProcessor} for the Gradle command execution
     *   This method may never returns {@code null}.
     */
    @Nonnull
    public TaskOutputProcessor startExecution(@Nonnull Project project);
}
