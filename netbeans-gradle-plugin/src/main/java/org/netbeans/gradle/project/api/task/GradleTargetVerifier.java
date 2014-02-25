package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.openide.windows.OutputWriter;

/**
 * Defines an operation which verifies if an associated task can be executed by
 * the given Gradle target. If this operation refuses the task to be executed,
 * the task execution is not considered to be successful, so
 * {@link ContextAwareCommandFinalizer} will not be executed.
 *
 * @see CustomCommandActions
 * @see ContextAwareGradleTargetVerifier
 */
public interface GradleTargetVerifier {
    /**
     * Checks if the associated task can be executed by the given Gradle target
     * or not. If this operation refuses the task to be executed,
     * the task execution is not considered to be successful, so
     * {@link ContextAwareCommandFinalizer} will not be executed.
     * <P>
     * This method also has an option to write to the output tab. For example,
     * if this method refuses to run the task, this method is responsible to
     * print why it was denied to the user.
     *
     * @param gradleTarget the Gradle target which is to be used to execute the
     *   associate task. This argument cannot be {@code null}.
     * @param output the {@code OutputWriter} which can be used to write messages
     *   to the output window associated with the Gradle command. This argument
     *   cannot be {@code null}.
     * @param errOutput the {@code OutputWriter} which can be used to write messages
     *   to the output window associated with the Gradle command as an error.
     *   This usually means that messages written to this writer will be printed
     *   in red. This argument cannot be {@code null}.
     * @return {@code true} if the task is to be executed, {@code false} if it
     *   must be skipped
     */
    public boolean checkTaskExecutable(
            @Nonnull GradleTarget gradleTarget,
            @Nonnull OutputWriter output,
            @Nonnull OutputWriter errOutput);
}
