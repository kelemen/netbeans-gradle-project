package org.netbeans.gradle.project.api.task;

import javax.annotation.Nullable;

/**
 * Defines a listener to be notified whenever a Gradle command has been
 * executed. The listener is guaranteed to be notified only if the command has
 * been attempted to be actually started.
 * <P>
 * If you need the context ({@code Project} and {@code Lookup}) for the listener,
 * you may use the {@link ContextAwareCommandAction} instead.
 * <P>
 * This listener is not notified more than once but might be notified on any
 * thread.
 * <B>Warning</B>: This listener is not going to be notified when executing a
 * task again (e.g.: "Repeat build" action). So the usefulness of this listener
 * is limited. Instead, {@link ContextAwareCommandCompleteAction} should be
 * preferred.
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
