package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines in what context has a particular Gradle command been executed.
 * Subsequent versions of Gradle Support might provide additional properties
 * in this class.
 * <P>
 * Instances of {@code ExecutedCommandContext} are immutable assuming that its
 * properties are immutable as well (as they should be).
 *
 * @see ContextAwareCommandCompleteAction
 * @see ContextAwareCommandCompleteListener
 */
public final class ExecutedCommandContext {
    private final TaskVariableMap taskVariables;

    /**
     * Creates a new {@code ExecutedCommandContext} with the task variables
     * used when the executed Gradle command has been resolved.
     *
     * @param taskVariables the task variables used when the executed Gradle
     *   command has been resolved. This argument cannot be {@code null}.
     */
    public ExecutedCommandContext(@Nonnull TaskVariableMap taskVariables) {
        ExceptionHelper.checkNotNullArgument(taskVariables, "taskVariables");

        this.taskVariables = taskVariables;
    }

    /**
     * Returns the task variables used when the executed Gradle command has
     * been resolved. The returned map contains variables entered by the user
     * as well. Also, it contains values of variables which could potentially
     * be used in the Gradle command (without asking the user for their values).
     *
     * @return the task variables used when the executed Gradle command has
     *   been resolved. This method never returns {@code null}.
     */
    @Nonnull
    public TaskVariableMap getTaskVariables() {
        return taskVariables;
    }
}
