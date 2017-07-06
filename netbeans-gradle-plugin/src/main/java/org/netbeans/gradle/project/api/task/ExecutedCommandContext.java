package org.netbeans.gradle.project.api.task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.tasks.vars.EmptyTaskVarMap;

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
    /**
     * A builder class used to create {@link ExecutedCommandContext} instances.
     * Once you have initialized all the properties, you need to call the
     * {@link #create() create} method to instantiate a new
     * {@code ExecutedCommandContext} instance.
     * <P>
     * Instances of this class may not be used concurrently but otherwise
     * <I>synchronization transparent</I>, so they might be used in any context.
     */
    public static final class Builder {
        private TaskVariableMap taskVariables;
        private List<String> taskNames;
        private List<String> arguments;
        private List<String> jvmArgument;

        /**
         * Creates a new builder with no task variables, task names, arguments and JVM arguments.
         */
        public Builder() {
            this.taskVariables = EmptyTaskVarMap.INSTANCE;
            this.taskNames = Collections.emptyList();
            this.arguments = Collections.emptyList();
            this.jvmArgument = Collections.emptyList();
        }

        /**
         * Sets the task variables used when the executed Gradle command had
         * been resolved.
         *
         * @param taskVariables the task variables used when the executed
         *   Gradle command had been resolved. This argument cannot be {@code null}.
         */
        public void setTaskVariables(@Nonnull TaskVariableMap taskVariables) {
            this.taskVariables = Objects.requireNonNull(taskVariables, "taskVariables");
        }

        /**
         * Sets the target tasks which were requested to be executed.
         *
         * @param taskNames the target tasks which were requested to be executed.
         *   This argument cannot be {@code null} and the list cannot contain
         *   {@code null} elements.
         */
        public void setTaskNames(@Nonnull List<String> taskNames) {
            this.taskNames = CollectionUtils.copyNullSafeList(taskNames);
        }

        /**
         * Sets the arguments of the executed Gradle command.
         *
         * @param arguments the arguments of the executed Gradle command.
         *   This argument cannot be {@code null} and the list cannot contain
         *   {@code null} elements.
         */
        public void setArguments(@Nonnull List<String> arguments) {
            this.arguments = CollectionUtils.copyNullSafeList(arguments);
        }

        /**
         * Sets the JVM arguments of the executed Gradle command.
         *
         * @param jvmArgument the JVM arguments of the executed Gradle command.
         *   This argument cannot be {@code null} and the list cannot contain
         *   {@code null} elements.
         */
        public void setJvmArgument(@Nonnull List<String> jvmArgument) {
            this.jvmArgument = CollectionUtils.copyNullSafeList(jvmArgument);
        }

        /**
         * Creates a new {@code ExecutedCommandContext} with the currently
         * specified properties for this builder. Subsequent adjustment to this
         * builder will have no effect on the returned instance.
         *
         * @return a new {@code ExecutedCommandContext} with the currently
         *   specified properties for this builder. This method never returns
         *   {@code null}.
         */
        public ExecutedCommandContext create() {
            return new ExecutedCommandContext(this);
        }
    }

    private final TaskVariableMap taskVariables;
    private final List<String> taskNames;
    private final List<String> arguments;
    private final List<String> jvmArgument;

    private ExecutedCommandContext(Builder builder) {
        this.taskVariables = builder.taskVariables;
        this.taskNames = builder.taskNames;
        this.arguments = builder.arguments;
        this.jvmArgument = builder.jvmArgument;
    }

    /**
     * Creates a new {@code ExecutedCommandContext} with the task variables
     * used when the executed Gradle command has been resolved.
     *
     * @param taskVariables the task variables used when the executed Gradle
     *   command has been resolved. This argument cannot be {@code null}.
     */
    public ExecutedCommandContext(@Nonnull TaskVariableMap taskVariables) {
        this(newBuilder(taskVariables));
    }

    private static Builder newBuilder(TaskVariableMap taskVariables) {
        Builder result = new Builder();
        result.setTaskVariables(taskVariables);
        return result;
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

    /**
     * Returns the names of the tasks which were requested to
     * be executed. This list only contains tasks which were explicitly
     * requested and is not a list of task which were actually executed.
     *
     * @return the names of the tasks which were requested to
     *   be executed. This method never returns {@code null} and
     *   the returned list does not contain {@code null} elements.
     */
    @Nonnull
    public List<String> getTaskNames() {
        return taskNames;
    }

    /**
     * Returns the arguments passed to the executed Gradle command.
     * This list does not contain arguments specified for the JVM
     * executing the Gradle command.
     *
     * @return the arguments passed to the executed Gradle command.
     *   This method never returns {@code null} and the returned list
     *   does not contain {@code null} elements.
     */
    @Nonnull
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns the arguments passed to the JVM executing the
     * Gradle command. That is, the arguments used to start the
     * Gradle daemon.
     *
     * @return the arguments passed to the JVM executing the Gradle command.
     *   This method never returns {@code null} and the returned list
     *   does not contain {@code null} elements.
     */
    @Nonnull
    public List<String> getJvmArgument() {
        return jvmArgument;
    }
}
