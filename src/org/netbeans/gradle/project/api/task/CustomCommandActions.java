package org.netbeans.gradle.project.api.task;

/**
 * Defines custom actions to be associated with a
 * {@link GradleCommandTemplate Gradle command}.
 * <P>
 * Note that you can only instantiate this class using its {@link Builder}.
 * For common actions, you may use the declared {@code static final} fields.
 * <P>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently without any synchronization.
 *
 * @see BuiltInGradleCommandQuery
 * @see GradleCommandExecutor
 */
public final class CustomCommandActions {
    /**
     * Defines an immutable {@code CustomCommandActions} with the
     * {@link #getTaskKind() task kind} of {@link TaskKind#BUILD}.
     * <P>
     * Aside from the task kind property all other properties are {@code null}.
     */
    public static final CustomCommandActions BUILD = new Builder(TaskKind.BUILD).create();

    /**
     * Defines an immutable {@code CustomCommandActions} with the
     * {@link #getTaskKind() task kind} of {@link TaskKind#RUN}.
     * <P>
     * Aside from the task kind property all other properties are {@code null}.
     */
    public static final CustomCommandActions RUN = new Builder(TaskKind.RUN).create();

    /**
     * Defines an immutable {@code CustomCommandActions} with the
     * {@link #getTaskKind() task kind} of {@link TaskKind#DEBUG}.
     * <P>
     * Aside from the task kind property all other properties are {@code null}.
     */
    public static final CustomCommandActions DEBUG = new Builder(TaskKind.DEBUG).create();

    /**
     * Defines an immutable {@code CustomCommandActions} with the
     * {@link #getTaskKind() task kind} of {@link TaskKind#OTHER}.
     * <P>
     * Aside from the task kind property all other properties are {@code null}.
     */
    public static final CustomCommandActions OTHER = new Builder(TaskKind.OTHER).create();

    /**
     * Defines a class which might be used to create
     * {@link CustomCommandActions} instances. To create new
     * {@code CustomCommandActions} instances: Create a new
     * {@code CustomCommandActions.Builder}, set the appropriate properties and
     * call {@link #create() create()}.
     * <P>
     * Methods of this class cannot be accessed concurrently but access to its
     * methods might be synchronized with an external lock.
     */
    public static final class Builder {
        private final TaskKind taskKind;
        private CommandCompleteListener commandCompleteListener;
        private TaskOutputProcessor stdOutProcessor;
        private TaskOutputProcessor stdErrProcessor;

        /**
         * Creates a new {@code Builder} with the specified task kind and with
         * all other properties initially set to {@code null}. By default, you
         * should use {@link TaskKind#BUILD}.
         *
         * @param taskKind the kind of task affecting the output window
         *   handling of the executed task. This argument cannot be
         *   {@code null}.
         *
         * @throws NullPointerException thrown if the specified task kind is
         *   {@code null}
         */
        public Builder(TaskKind taskKind) {
            if (taskKind == null) throw new NullPointerException("taskKind");

            this.taskKind = taskKind;
            this.commandCompleteListener = null;
            this.stdOutProcessor = null;
            this.stdErrProcessor = null;
        }

        /**
         * Returns the kind of tasks affecting the output window handling of the
         * executed task.
         *
         * @return the kind of tasks affecting the output window handling of the
         *   executed task. This method may never return {@code null}.
         */
        public TaskKind getTaskKind() {
            return taskKind;
        }

        /**
         * Returns the {@code CommandCompleteListener} last set by the
         * {@link #setCommandCompleteListener(CommandCompleteListener) setCommandCompleteListener}
         * method.
         * <P>
         * The default value for this property is {@code null}.
         * <P>
         * The {@code CommandCompleteListener} is executed after the associated
         * Gradle commands completes.
         *
         * @return the {@code CommandCompleteListener} last set by the
         *   {@link #setCommandCompleteListener(CommandCompleteListener) setCommandCompleteListener}
         *   method. This method may return {@code null} if {@code null} was
         *   set.
         */
        public CommandCompleteListener getCommandCompleteListener() {
            return commandCompleteListener;
        }

        /**
         * Sets the {@code CommandCompleteListener} to be executed after the
         * associated Gradle command completes. An invocation of this method
         * overwrites previous invocation of this method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after the Gradle command completes.
         *
         * @param commandCompleteListener the {@code CommandCompleteListener} to
         *   be executed after the associated Gradle command completes. This
         *   argument can be {@code null}, if there is nothing to do after the
         *   Gradle command completes.
         */
        public void setCommandCompleteListener(CommandCompleteListener commandCompleteListener) {
            this.commandCompleteListener = commandCompleteListener;
        }

        /**
         * Returns the {@code TaskOutputProcessor} last set by the
         * {@link #setStdOutProcessor(TaskOutputProcessor) setStdOutProcessor}
         * method.
         * <P>
         * The default value for this property is {@code null}.
         * <P>
         * The {@code TaskOutputProcessor} is called after each line written to
         * the standard output by the associated Gradle command.
         *
         * @return the {@code TaskOutputProcessor} last set by the
         *   {@link #setStdOutProcessor(TaskOutputProcessor) setStdOutProcessor}
         *   method. This method may return {@code null} if {@code null} was
         *   set.
         */
        public TaskOutputProcessor getStdOutProcessor() {
            return stdOutProcessor;
        }

        /**
         * Sets the {@code TaskOutputProcessor} to be called after each line
         * written to the standard output by the associated Gradle command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard output.
         *
         * @param stdOutProcessor the {@code TaskOutputProcessor} to be called
         *   after each line written to the standard output by the associated
         *   Gradle command. This argument can be {@code null}, if there is
         *   nothing to do after lines are written to the standard output.
         */
        public void setStdOutProcessor(TaskOutputProcessor stdOutProcessor) {
            this.stdOutProcessor = stdOutProcessor;
        }

        /**
         * Returns the {@code TaskOutputProcessor} last set by the
         * {@link #setStdErrProcessor(TaskOutputProcessor) setStdErrProcessor}
         * method.
         * <P>
         * The default value for this property is {@code null}.
         * <P>
         * The {@code TaskOutputProcessor} is called after each line written to
         * the standard error by the associated Gradle command.
         *
         * @return the {@code TaskOutputProcessor} last set by the
         *   {@link #setStdErrProcessor(TaskOutputProcessor) setStdErrProcessor}
         *   method. This method may return {@code null} if {@code null} was
         *   set.
         */
        public TaskOutputProcessor getStdErrProcessor() {
            return stdErrProcessor;
        }

        /**
         * Sets the {@code TaskOutputProcessor} to be called after each line
         * written to the standard error by the associated Gradle command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard error.
         *
         * @param stdErrProcessor the {@code TaskOutputProcessor} to be called
         *   after each line written to the standard error by the associated
         *   Gradle command. This argument can be {@code null}, if there is
         *   nothing to do after lines are written to the standard error.
         */
        public void setStdErrProcessor(TaskOutputProcessor stdErrProcessor) {
            this.stdErrProcessor = stdErrProcessor;
        }

        /**
         * Creates a new {@code CustomCommandActions} instances with the
         * properties currently set for this builder. Subsequent modifications
         * to this builder has no effect on the returned instance.
         *
         * @return a new {@code CustomCommandActions} instances with the
         *   properties currently set for this builder. This method may never
         *   return {@code null}.
         */
        public CustomCommandActions create() {
            return new CustomCommandActions(this);
        }
    }

    private final TaskKind taskKind;
    private final CommandCompleteListener commandCompleteListener;
    private final TaskOutputProcessor stdOutProcessor;
    private final TaskOutputProcessor stdErrProcessor;

    private CustomCommandActions(Builder builder) {
        this.taskKind = builder.getTaskKind();
        this.commandCompleteListener = builder.getCommandCompleteListener();
        this.stdOutProcessor = builder.getStdOutProcessor();
        this.stdErrProcessor = builder.getStdErrProcessor();
    }

    /**
     * Returns the kind of tasks affecting the output window handling of the
     * executed task.
     *
     * @return the kind of tasks affecting the output window handling of the
     *   executed task. This method may never return {@code null}.
     */
    public TaskKind getTaskKind() {
        return taskKind;
    }

    /**
     * Returns the {@code CommandCompleteListener} to be executed after the
     * associated Gradle commands completes or {@code null} if there is no
     * action to be taken.
     *
     * @return the {@code CommandCompleteListener} to be executed after the
     *   associated Gradle commands completes or {@code null} if there is no
     *   action to be taken
     */
    public CommandCompleteListener getCommandCompleteListener() {
        return commandCompleteListener;
    }

    /**
     * Returns the {@code TaskOutputProcessor} to be called after each line
     * written to the standard output by the associated Gradle command or
     * {@code null} if there is nothing to do after lines are written to the
     * standard output.
     *
     * @return the {@code TaskOutputProcessor} to be called after each line
     *   written to the standard output by the associated Gradle command or
     *   {@code null} if there is nothing to do after lines are written to the
     *   standard output
     */
    public TaskOutputProcessor getStdOutProcessor() {
        return stdOutProcessor;
    }

    /**
     * Returns the {@code TaskOutputProcessor} to be called after each line
     * written to the standard error by the associated Gradle command or
     * {@code null} if there is nothing to do after lines are written to the
     * standard error.
     *
     * @return the {@code TaskOutputProcessor} to be called after each line
     *   written to the standard error by the associated Gradle command or
     *   {@code null} if there is nothing to do after lines are written to the
     *   standard error
     */
    public TaskOutputProcessor getStdErrProcessor() {
        return stdErrProcessor;
    }
}
