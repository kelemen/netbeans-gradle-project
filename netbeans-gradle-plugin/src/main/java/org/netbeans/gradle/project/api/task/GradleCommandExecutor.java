package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;

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
 * This executor is available on the
 * {@link org.netbeans.api.project.Project#getLookup() project's lookup}. It is
 * already available when loading extensions (i.e., maybe retrieved in the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#createExtension(org.netbeans.api.project.Project) GradleProjectExtensionDef.createExtension}
 * method.
 *
 * @see GradleCommandTemplate
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#createExtension(org.netbeans.api.project.Project)
 */
public interface GradleCommandExecutor {
    /**
     * Executes the specified Gradle command sometime in the future on a
     * background thread. The command might be canceled by the user (though
     * cancellation support has limited usefulness until Gradle does not support
     * canceling tasks), so it is possible that it will never get executed.
     * <P>
     * Custom actions are passed to define additional behaviour for the executed
     * Gradle command.
     *
     * @param command the Gradle command to be executed. This argument cannot be
     *   {@code null}.
     * @param customActions the custom actions associated with the specified
     *   Gradle command. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public void executeCommand(
            @Nonnull GradleCommandTemplate command,
            @Nonnull CustomCommandActions customActions);
}
