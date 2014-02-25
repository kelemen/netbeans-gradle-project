package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;

/**
 * Defines a single method which is called after each line output by an executed
 * Gradle command.
 * <P>
 * Instances of this interface are not required to be safe to be accessed by
 * multiple threads concurrently. This interface must expect to be called from
 * any context, so should be <I>synchronization transparent</I>.
 *
 * @see CustomCommandActions
 * @see SingleExecutionOutputProcessor
 */
public interface TaskOutputProcessor {
    /**
     * Called after each line written to the output of a Gradle command.
     * <P>
     * Note that this method must be as efficient as possible because it might
     * block tasks from being executed.
     *
     * @param line the line written to the output. This argument cannot be
     *   {@code null}.
     */
    public void processLine(@Nonnull String line);
}
