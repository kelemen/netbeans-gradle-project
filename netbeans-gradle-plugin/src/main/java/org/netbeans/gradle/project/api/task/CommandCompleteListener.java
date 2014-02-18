package org.netbeans.gradle.project.api.task;

import javax.annotation.Nullable;

/**
 * Defines a listener to be notified whenever a Gradle command has been
 * executed. The listener is guaranteed to be notified only if the command has
 * been attempted to be actually started. But after that, this listener
 * might be notified multiple times if the task is executed again and the
 * context did not change. This is typical for repeat build and similar actions.
 * <P>
 * If you need the context ({@code Project} and {@code Lookup}) for the listener,
 * you may use the {@link ContextAwareCommandAction} instead.
 * <P>
 * This listener might be notified on any thread.
 *
 * @see GradleCommandExecutor
 * @see ContextAwareCommandCompleteAction
 */
public interface CommandCompleteListener {
    /**
     * The method to be called whenever a Gradle command has been
     * executed.
     *
     * @param error the exception thrown when attempting to execute the Gradle
     *   command. This argument is {@code null} if no error occurred. Also, in
     *   some cases this argument is {@code null} if the task has been canceled.
     */
    public void onComplete(@Nullable Throwable error);
}
