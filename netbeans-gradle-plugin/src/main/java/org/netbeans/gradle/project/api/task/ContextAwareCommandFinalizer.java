package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.openide.windows.OutputWriter;

/**
 * Defines the task to be executed after a {@link GradleCommandTemplate Gradle command}
 * completes successfully. Instances of this interface are expected to be
 * created by a {@link ContextAwareCommandAction}.
 * <P>
 * The method of this interface is called from a background thread (not the Event Dispatch Thread).
 *
 * @see ContextAwareCommandAction
 */
public interface ContextAwareCommandFinalizer {
    /**
     * Called when a Gradle command completes successfully.
     *
     * @param output the {@code OutputWriter} which can be used to write messages
     *   to the output window associated with the Gradle command. This argument
     *   cannot be {@code null}.
     * @param errOutput the {@code OutputWriter} which can be used to write messages
     *   to the output window associated with the Gradle command as an error.
     *   This usually means that messages written to this writer will be printed
     *   in red. This argument cannot be {@code null}.
     */
    public void finalizeSuccessfulCommand(
            @Nonnull OutputWriter output,
            @Nonnull OutputWriter errOutput);
}
