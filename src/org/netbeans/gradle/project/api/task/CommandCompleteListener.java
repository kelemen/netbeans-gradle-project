package org.netbeans.gradle.project.api.task;

/**
 * Defines a listener to be notified whenever a Gradle command has been
 * executed. The listener is guaranteed to be notified only if the command has
 * been attempted to be actually started.
 * <P>
 * This listener is not notified more than once but might be notified on any
 * thread.
 *
 * @see GradleCommandExecutor
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
    public void onComplete(Throwable error);
}
