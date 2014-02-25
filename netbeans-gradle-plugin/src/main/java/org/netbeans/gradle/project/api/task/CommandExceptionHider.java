package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;

/**
 * Defines a check to be used if an exception thrown by the associated Gradle
 * command should be hidden or not. That is, if the specified exception hider
 * returns {@code true} (meaning that the exception must be hidden), then the
 * exception will not be displayed to the user (it will still be logged on
 * {@code INFO} level).
 * <P>
 * The intent of this interface is to allow extensions to replace the default
 * error message with a more meaningful one.
 *
 * @see CustomCommandActions
 * @see GradleCommandExecutor
 */
public interface CommandExceptionHider {
    /**
     * Returns {@code true} if the passed exception is not to be displayed
     * as a build error by the Gradle Support plugin.
     * <P>
     * Note that if this method returns {@code true}, the exception will still
     * be logged (on {@code INFO} level).
     *
     * @param taskError the exception thrown by the executed Gradle command.
     *   This argument cannot be {@code null}.
     * @return {@code true} if the exception is not to be displayed to the user,
     *   {@code false} if the exception is displayed to the user using the
     *   default handlers
     */
    public boolean hideException(@Nonnull Throwable taskError);
}
