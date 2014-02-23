package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Defines an operation (based on the starting context) which verifies if an
 * associated task can be executed by the given Gradle target. If this operation
 * refuses the task to be executed, the task execution is not considered to be
 * successful, so {@link ContextAwareCommandFinalizer} will not be executed.
 *
 * @see CustomCommandActions
 * @see GradleTargetVerifier
 */
public interface ContextAwareGradleTargetVerifier {
    /**
     * Called before a Gradle command is executed and returns the
     * {@code GradleTargetVerifier} used to check if the given Gradle target is
     * able to execute the command.
     * <P>
     * If the command to be executed is a built-in task, then the
     * {@code commandContext} is the {@code Lookup}, NetBeans passes to the
     * {@link org.netbeans.spi.project.ActionProvider}. Otherwise it is an empty
     * {@code Lookup}.
     * <P>
     * Note: If it can be determined from the context that any version of Gradle
     * is able to execute the associated command, it is recommended to return
     * {@code null}, because in this case the command executor will not need to
     * fetch the version of Gradle.
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
     * @return the {@code GradleTargetVerifier} used to check if the given
     *   Gradle target is able to execute the command. May return {@code null}
     *   if any version of Gradle will be able to run the associated command.
     *
     * @see NbCommandString
     */
    @Nullable
    public GradleTargetVerifier startCommand(
            @Nonnull Project project,
            @Nonnull Lookup commandContext);
}
