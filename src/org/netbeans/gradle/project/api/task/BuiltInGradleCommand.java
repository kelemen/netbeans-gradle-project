package org.netbeans.gradle.project.api.task;

/**
 * Defines a command to be executed for built-in commands (such as build).
 * That is, instances of this interface are associated with a command as defined
 * by {@link org.netbeans.spi.project.ActionProvider} interface.
 * <P>
 * Commands are defined by a {@link #getDefaultGradleCommand() Gradle command}
 * and {@link #getAfterCommandAction() an arbitrary action} to be executed after
 * the Gradle command completes. Note that the associated Gradle command may be
 * redefined by users.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see BuiltInGradleCommandQuery
 */
public interface BuiltInGradleCommand {
    /**
     * Defines the default Gradle command to run when the built-in command is to
     * be executed. This command might be reconfigured by users, so there is no
     * guarantee that this command will actually be executed.
     *
     * @return the default Gradle command to run when the built-in command is to
     *   be executed. This method may return {@code null} in the rare case when
     *   no Gradle command needs to be executed.
     */
    public GradleCommandTemplate getDefaultGradleCommand();

    /**
     * Returns the action to be executed after the associated Gradle command
     * completes.
     *
     * @return the action to be executed after the associated Gradle command
     *   completes. This method may return {@code null}, if no associated task
     *   needs to be executed after the Gradle command.
     */
    public CommandCompleteListener getAfterCommandAction();
}
