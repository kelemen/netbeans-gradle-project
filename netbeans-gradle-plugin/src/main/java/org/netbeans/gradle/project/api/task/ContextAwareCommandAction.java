package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Defines a custom action for a {@link GradleCommandTemplate Gradle command}
 * which needs the {@link Lookup} context and the {@link Project} instance
 * associated with the currently started task.
 * <P>
 * If you need to be notified even on failure, you may use the
 * {@link ContextAwareCommandCompleteAction}.
 * <P>
 * The method of this interface is called from a background thread (not the Event Dispatch Thread).
 *
 * @see BuiltInGradleCommandQuery
 * @see ContextAwareCommandCompleteAction
 * @see CustomCommandActions
 * @see GradleCommandExecutor
 */
public interface ContextAwareCommandAction {
    /**
     * Called before a Gradle command is executed and returns the
     * {@link ContextAwareCommandFinalizer} which is to be notified if the
     * Gradle command completes successfully.
     * <P>
     * If the command to be executed is a built-in task, then the
     * {@code commandContext} is the {@code Lookup}, NetBeans passes to the
     * {@link org.netbeans.spi.project.ActionProvider}. Otherwise it is an empty
     * {@code Lookup}.
     * <P>
     * Note that {@code Lookup} will always contain an instance of
     * {@link NbCommandString} which specifies the command string passed to the
     * {@link org.netbeans.spi.project.ActionProvider ActionProvider} implementation.
     *
     * @param project the Gradle project in which context the command is
     *   executed. This is similar to executing a command from the command line
     *   from the directory of this project. This argument cannot be {@code null}.
     * @param commandContext the context when the command was started. This
     *   argument cannot be {@code null}.
     *
     * @return the {@link ContextAwareCommandFinalizer} which is to be notified
     *   if the Gradle command completes successfully. This method may never
     *   return {@code null}.
     *
     * @see NbCommandString
     */
    @Nonnull
    public ContextAwareCommandFinalizer startCommand(@Nonnull Project project, @Nonnull Lookup commandContext);
}
