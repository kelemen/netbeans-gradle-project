package org.netbeans.gradle.project.api.task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines a Gradle command which might also contain
 * {@link TaskVariable variable templates}. Variable templates might be added
 * to task names, arguments and JVM arguments as well.
 * <P>
 * Instances of this class cannot be created directly but via a {@link Builder}.
 * <P>
 * Instances of {@code GradleCommandTemplate} are immutable and as such are safe
 * to be accessed concurrently without any synchronization.
 *
 * @see GradleCommandExecutor
 */
public final class GradleCommandTemplate {
    /**
     * A builder class used to create {@link GradleCommandTemplate} instances.
     * Once you have initialized all the properties, you need to call the
     * {@link #create() create} method to instantiate a new
     * {@code GradleCommandTemplate} instance.
     * <P>
     * Instances of this class may not be used concurrently but otherwise
     * <I>synchronization transparent</I>, so they might be used in any context.
     */
    public static final class Builder {
        private final String displayName;
        private final List<String> tasks;
        private List<String> arguments;
        private List<String> jvmArguments;
        private boolean blocking;

        /**
         * Creates a new builder whose properties are initially set the
         * appropriate properties of the specified {@code GradleCommandTemplate}.
         *
         * @param command the {@code GradleCommandTemplate} whose properties
         *   are copied to this builder. This argument cannot be {@code null}.
         *
         * @throws NullPointerException thrown if the specified command is
         *   {@code null}
         */
        public Builder(@Nonnull GradleCommandTemplate command) {
            this.displayName = command.getDisplayName();
            this.tasks = command.getTasks();
            this.arguments = command.getArguments();
            this.jvmArguments = command.getJvmArguments();
            this.blocking = command.isBlocking();
        }

        /**
         * Creates a new {@code Builder} with the given task names.
         * The default value for the other properties are the following:
         * <ul>
         *  <li>{@link #getArguments() Arguments}: Empty list.</li>
         *  <li>{@link #getJvmArguments() JvmArguments}: Empty list.</li>
         *  <li>{@link #isBlocking() Blocking}: {@code true}.</li>
         * </ul>
         *
         * @param displayName the name of the command as displayed to the user.
         *   This argument cannot be {@code null} but can be an empty string
         *   which implies that some other sensible value is to be used
         *   (like the list of the tasks).
         * @param tasks the list of tasks to be executed by Gradle. This list
         *   cannot contain {@code null} elements nor can it be an empty list.
         *
         * @throws NullPointerException thrown if the specified list is
         *   {@code null} or contains {@code null} elements or the display name
         *   is {@code null}.
         * @throws IllegalArgumentException thrown if the specified list is
         *   empty
         */
        public Builder(
                @Nonnull String displayName,
                @Nonnull List<String> tasks) {

            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.tasks = CollectionUtils.copyNullSafeList(tasks);
            this.arguments = Collections.emptyList();
            this.jvmArguments = Collections.emptyList();
            this.blocking = true;

            if (this.tasks.isEmpty()) {
                throw new IllegalArgumentException("Must have at least a single task specified.");
            }
        }

        /**
         * Sets the arguments specified for the Gradle command. This method call
         * overwrites the values set by previous {@code setArguments} calls. The
         * arguments may contain {@link TaskVariable variables to be replaced}.
         * <P>
         * The default value for this property if unset is an empty list.
         * <P>
         * Note: These arguments are specified for Gradle itself and not for the
         * JVM executing Gradle. JVM arguments need to be specified via the
         * {@link #setJvmArguments(List) JvmArguments} property.
         *
         * @param arguments the arguments to be specified for Gradle. This list
         *   cannot be {@code null} and cannot contain {@code null} elements
         *   but may be empty.
         *
         * @throws NullPointerException thrown if the specified list is
         *   {@code null} or contains {@code null} elements
         */
        public void setArguments(@Nonnull List<String> arguments) {
            this.arguments = CollectionUtils.copyNullSafeList(arguments);
        }

        /**
         * Sets the arguments for the JVM running Gradle. This method call
         * overwrites the values set by previous {@code setJvmArguments} calls.
         * The arguments may contain {@link TaskVariable variables to be replaced}.
         * <P>
         * Note: Users may specify additional JVM arguments in the global
         * settings and these JVM arguments will be added regardless what is
         * specified in this list.
         * <P>
         * <B>Warning</B>: Specifying different JVM arguments for different
         * commands are likely to spawn a new Gradle daemon. Note that the
         * Gradle daemon is a long lived process and by default has a
         * considerable memory footprint. Therefore, spawning new Gradle daemons
         * should be avoided if possible.
         *
         * @param jvmArguments the arguments for the JVM running Gradle. This
         *   list cannot be {@code null} and cannot contain {@code null}
         *   elements but may be empty.
         *
         * @throws NullPointerException thrown if the specified list is
         *   {@code null} or contains {@code null} elements
         */
        public void setJvmArguments(@Nonnull List<String> jvmArguments) {
            this.jvmArguments = CollectionUtils.copyNullSafeList(jvmArguments);
        }

        /**
         * Sets if this Gradle command might block other commands indefinitely
         * or not. This method call overwrites the values set by previous
         * {@code setBlocking} calls.
         * <P>
         * Gradle commands will be executed so that if a task is not blocking
         * (this method returns {@code false}), then all subsequent tasks will
         * wait for the non-blocking task to complete. The reason for this
         * distinction is to prevent accidentally starting multiple Gradle
         * daemons if multiple Gradle commands are scheduled concurrently.
         * <P>
         * An example for a blocking task is "debug" and "build" should usually
         * be considered as a non-blocking task.
         *
         * @param blocking {@code true} if this Gradle command might block other
         *   commands indefinitely, {@code false} otherwise
         */
        public void setBlocking(boolean blocking) {
            this.blocking = blocking;
        }

        /**
         * Creates a new {@code GradleCommandTemplate} with the currently
         * specified properties for this builder. Subsequent adjustment to this
         * builder will have no effect on the returned instance.
         *
         * @return a new {@code GradleCommandTemplate} with the currently
         *   specified properties for this builder. This method never returns
         *   {@code null}.
         */
        @Nonnull
        public GradleCommandTemplate create() {
            return new GradleCommandTemplate(this);
        }
    }

    private final String displayName;
    private final List<String> tasks;
    private final List<String> arguments;
    private final List<String> jvmArguments;
    private final boolean blocking;

    private GradleCommandTemplate(Builder builder) {
        this.displayName = builder.displayName;
        this.tasks = builder.tasks;
        this.arguments = builder.arguments;
        this.jvmArguments = builder.jvmArguments;
        this.blocking = builder.blocking;
    }

    /**
     * Returns the display name of the command to be executed. This display name
     * can be used in progress text and output window captions.
     * <P>
     * In case this method returns an empty a string a sensible default value
     * is to be used.
     *
     * @return the display name of the command to be executed. This method
     *   never returns {@code null}.
     *
     * @see #getSafeDisplayName()
     */
    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the display name of the command to be executed or a sensible
     * default when the display name is an empty string. This display name
     * can be used in progress text and output window captions.
     * <P>
     * In case this method returns an empty a string a sensible default value
     * is to be used.
     *
     * @return the display name of the command to be executed. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public String getSafeDisplayName() {
        return displayName.isEmpty() ? tasks.toString() : displayName;
    }

    /**
     * Returns the list of tasks to be executed by Gradle. Tasks will be
     * executed in the order they were specified if possible without
     * violating task dependencies. The task names may contain
     * {@link TaskVariable variables to be replaced}.
     *
     * @return the list of tasks to be executed by Gradle. This method never
     *   returns {@code null}, the returned list does not contain
     *   {@code null} elements and is never empty.
     */
    @Nonnull
    public List<String> getTasks() {
        return tasks;
    }

    /**
     * Returns the arguments for this Gradle command.
     * <P>
     * Note: These arguments are specified for Gradle itself and not for the
     * JVM executing Gradle. JVM arguments need to be specified via the
     * {@link #getJvmArguments() JvmArguments} property.
     *
     * @return the arguments for this Gradle command. This method
     *   never returns {@code null} and the returned list does not contain
     *   {@code null} elements but may be empty.
     */
    @Nonnull
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns the JVM arguments for this Gradle command.
     * These arguments are used to start the JVM process executing Gradle
     * which is executing the Gradle command.
     * <P>
     * Note: Users may specify additional JVM arguments in the global
     * settings and these JVM arguments will be added regardless what is
     * specified in this list.
     * <P>
     * <B>Warning</B>: Specifying different JVM arguments for different
     * commands are likely to spawn a new Gradle daemon. Note that the
     * Gradle daemon is a long lived process and by default has a
     * considerable memory footprint. Therefore, spawning new Gradle daemons
     * should be avoided if possible.
     *
     * @return the last set JVM arguments for this Gradle command. This
     *   method never returns {@code null} and the returned list does not
     *   contain {@code null} elements but may be empty.
     */
    @Nonnull
    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    /**
     * Returns {@code true} if this Gradle command might block other
     * commands indefinitely.
     * <P>
     * Gradle commands will be executed so that if a task is not blocking
     * (this method returns {@code false}), then all subsequent tasks will
     * wait for the non-blocking task to complete. The reason for this
     * distinction is to prevent accidentally starting multiple Gradle
     * daemons if multiple Gradle commands are scheduled concurrently.
     * <P>
     * An example for a blocking task is "debug". An example for a non-blocking
     * task is "build".
     *
     * @return {@code true} if this Gradle command might block other
     *   commands indefinitely, {@code false} otherwise
     */
    public boolean isBlocking() {
        return blocking;
    }
}
