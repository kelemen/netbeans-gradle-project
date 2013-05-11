package org.netbeans.gradle.project.api.task;

/**
 * Defines an executor which is able to execute Gradle commands. The commands
 * are executed on a background thread. Note that there is a limit on how much
 * task might be executed concurrently. This limit is currently set to 10 but
 * may be increased in the future.
 * <P>
 * This API does not currently allow for cancellation because as of now Gradle
 * does not support canceling command execution. When Gradle will support
 * cancellation a new executor might be added to the project's lookup.
 * <P>
 * This executed is available on the
 * {@link org.netbeans.api.project.Project#getLookup() project's lookup}. It is
 * already available when loading extensions (i.e., maybe retrieved in the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery#loadExtensionForProject(org.netbeans.api.project.Project) GradleProjectExtensionQuery.loadExtensionForProject}
 * method.
 *
 * @see GradleCommandTemplate
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery#loadExtensionForProject(org.netbeans.api.project.Project)
 */
public interface GradleCommandExecutor {
    /**
     * Executes the specified Gradle command sometime in the future on a
     * background thread. The command might be canceled by the user (though
     * cancellation support has limited usefulness until Gradle does not support
     * canceling tasks), so it is possible that it will never get executed.
     * <P>
     * This method call is effectively equivalent to calling the two arguments
     * {@code executeCommand} with a {@link CommandCompleteListener} which does
     * nothing.
     *
     * @param command the Gradle command to be executed. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified command is
     *   {@code null}
     */
    public void executeCommand(GradleCommandTemplate command);

    /**
     * Executes the specified Gradle command sometime in the future on a
     * background thread. The command might be canceled by the user (though
     * cancellation support has limited usefulness until Gradle does not support
     * canceling tasks), so it is possible that it will never get executed.
     * <P>
     * You pass a listener to this method which is notified whenever the command
     * has completed. If the command is actually attempted to be executed, the
     * listener is guaranteed to be notified. Otherwise, <B>there is no strong
     * guarantee that the listener will ever be notified.</B> Therefore you
     * should never wait for
     * {@link CommandCompleteListener#onComplete(Throwable) CommandCompleteListener.onComplete}
     * to be called.
     * <P>
     * The specified listener may only be notified at most once.
     *
     * @param command the Gradle command to be executed. This argument cannot be
     *   {@code null}.
     * @param completeListener the listener to be notified if the Gradle command
     *   has termined (even if by failing with an exception). This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public void executeCommand(GradleCommandTemplate command, CommandCompleteListener completeListener);
}
