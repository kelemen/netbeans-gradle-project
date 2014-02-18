
package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a listener to be notified whenever a Gradle command has been
 * executed with the properties of the task actually executed. The listener is
 * guaranteed to be notified only if the command has been attempted to be
 * actually started. But after that, this listener might be notified multiple
 * times if the task is executed again and the context did not change. This is
 * typical for repeat build and similar actions.
 * <P>
 * This listener might be notified on any thread.
 *
 * @see ContextAwareCommandCompleteAction
 */
public interface ContextAwareCommandCompleteListener {
    /**
     * The method to be called whenever a Gradle command has been
     * executed.
     *
     * @param executedCommandContext the context containing information about
     *   the task which has been executed. This argument cannot be {@code null}.
     * @param error the exception thrown when attempting to execute the Gradle
     *   command. This argument is {@code null} if no error occurred. Also, in
     *   some cases this argument is {@code null} if the task has been canceled.
     */
    public void onComplete(
            @Nonnull ExecutedCommandContext executedCommandContext,
            @Nullable Throwable error);
}
