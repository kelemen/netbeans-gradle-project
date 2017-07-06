package org.netbeans.gradle.project.api.task;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;

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
    private static final Logger LOGGER = Logger.getLogger(CustomCommandActions.class.getName());

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
        private ContextAwareCommandArguments contextAwareCommandArguments;
        private CommandCompleteListener commandCompleteListener;
        private TaskOutputProcessor stdOutProcessor;
        private TaskOutputProcessor stdErrProcessor;
        private SingleExecutionOutputProcessor singleExecutionStdOutProcessor;
        private SingleExecutionOutputProcessor singleExecutionStdErrProcessor;
        private ContextAwareCommandAction contextAwareAction;
        private ContextAwareCommandCompleteAction contextAwareFinalizer;
        private GradleTargetVerifier gradleTargetVerifier;
        private ContextAwareGradleTargetVerifier contextAwareGradleTargetVerifier;
        private CommandExceptionHider commandExceptionHider;
        private CancellationToken cancelToken;
        private GradleCommandServiceFactory commandServiceFactory;

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
        public Builder(@Nonnull TaskKind taskKind) {
            this.taskKind = Objects.requireNonNull(taskKind, "taskKind");
            this.commandCompleteListener = null;
            this.stdOutProcessor = null;
            this.stdErrProcessor = null;
            this.contextAwareAction = null;
            this.contextAwareFinalizer = null;
            this.commandExceptionHider = null;
            this.gradleTargetVerifier = null;
            this.contextAwareGradleTargetVerifier = null;
            this.contextAwareCommandArguments = null;
            this.singleExecutionStdOutProcessor = null;
            this.singleExecutionStdErrProcessor = null;
            this.cancelToken = Cancellation.UNCANCELABLE_TOKEN;
            this.commandServiceFactory = null;
        }

        /**
         * Sets the {@code GradleCommandServiceFactory} which might define a
         * service which lives during the execution of the associated Gradle command.
         * The service may also provide {@link TaskVariable task variables} as
         * a last chance to replace task variables in Gradle command templates.
         * <P>
         * The default value of this property is {@code null}.
         *
         * @param commandServiceFactory the {@code GradleCommandServiceFactory}
         *   which might define a service which lives during the execution of
         *   the associated Gradle command. This argument can be {@code null}
         *   if no such service is required.
         */
        public void setCommandServiceFactory(@Nullable GradleCommandServiceFactory commandServiceFactory) {
            this.commandServiceFactory = commandServiceFactory;
        }

        /**
         * Sets a {@code CancellationToken} which might signal that the build
         * is to be terminated without needing to complete.
         * <P>
         * The default value for this property never signals cancellation.
         *
         * @param cancelToken the {@code CancellationToken} signalling cancellation
         *   when the build is to be terminated. This argument cannot be
         *   {@code null}.
         */
        public void setCancelToken(@Nonnull CancellationToken cancelToken) {
            this.cancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
        }

        /**
         * Specified the {@code ContextAwareCommandArguments} which is used
         * to provide additional {@link GradleCommandTemplate#getArguments() arguments}
         * for the Gradle command to be executed. The arguments will be appended
         * to the argument list after what is specified by
         * {@link GradleCommandTemplate}.
         * <P>
         * The additional arguments are processed the same way as if they were
         * specified in the associated {@code GradleCommandTemplate}.
         *
         * @param contextAwareCommandArguments the {@code ContextAwareCommandArguments}
         *   which is used to provide additional arguments for the Gradle
         *   command to be executed. This argument can be {@code null}, if no
         *   additional arguments are needed.
         */
        public void setContextAwareCommandArguments(@Nonnull ContextAwareCommandArguments contextAwareCommandArguments) {
            this.contextAwareCommandArguments = contextAwareCommandArguments;
        }

        /**
         * Sets a check to be used if an exception thrown by the associated
         * Gradle command should be hidden or not. That is, if the specified
         * exception hider returns {@code true} (meaning that the exception
         * must be hidden), then the exception will not be displayed to the user
         * (it will still be logged on {@code INFO} level).
         * <P>
         * Note that this check may also display the error to the user in other
         * forms (better describing what happened to the user).
         * <P>
         * You may set this property to {@code null}, if no such check
         * is needed. That is, exceptions should be displayed to the user
         * according to the best judgment of Gradle Supper. The default value
         * for this property is {@code null}.
         *
         * @param commandExceptionHider a check to be used if an exception
         *   thrown by the associated Gradle command should be hidden or not.
         *   This argument can be {@code null}, if no exception is needed to be
         *   hidden.
         */
        public void setCommandExceptionHider(@Nullable CommandExceptionHider commandExceptionHider) {
            this.commandExceptionHider = commandExceptionHider;
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
        public void setCommandCompleteListener(@Nullable CommandCompleteListener commandCompleteListener) {
            this.commandCompleteListener = commandCompleteListener;
        }

        /**
         * Sets the {@code TaskOutputProcessor} to be called after each line
         * written to the standard output by the associated Gradle command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard output.
         * <P>
         * <B>Warning</B>: Note that the passed {@code TaskOutputProcessor}
         * can be called for multiple tasks (even concurrently), therefore it
         * should not have a state. If you need state, you might want to use
         * {@link #setSingleExecutionStdOutProcessor(SingleExecutionOutputProcessor)}.
         *
         * @param stdOutProcessor the {@code TaskOutputProcessor} to be called
         *   after each line written to the standard output by the associated
         *   Gradle command. This argument can be {@code null}, if there is
         *   nothing to do after lines are written to the standard output.
         *
         * @see #setSingleExecutionStdOutProcessor(SingleExecutionOutputProcessor)
         */
        public void setStdOutProcessor(@Nullable TaskOutputProcessor stdOutProcessor) {
            this.stdOutProcessor = stdOutProcessor;
        }

        /**
         * Sets the {@code TaskOutputProcessor} to be called after each line
         * written to the standard error by the associated Gradle command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard error.
         * <P>
         * <B>Warning</B>: Note that the passed {@code TaskOutputProcessor}
         * can be called for multiple tasks (even concurrently), therefore it
         * should not have a state. If you need state, you might want to use
         * {@link #setSingleExecutionStdErrProcessor(SingleExecutionOutputProcessor)}.
         *
         * @param stdErrProcessor the {@code TaskOutputProcessor} to be called
         *   after each line written to the standard error by the associated
         *   Gradle command. This argument can be {@code null}, if there is
         *   nothing to do after lines are written to the standard error.
         *
         * @see #setSingleExecutionStdErrProcessor(SingleExecutionOutputProcessor)
         */
        public void setStdErrProcessor(@Nullable TaskOutputProcessor stdErrProcessor) {
            this.stdErrProcessor = stdErrProcessor;
        }

        /**
         * Sets a factory of {@link TaskOutputProcessor} processing the standard
         * output of the associated Gradle command. The factory is asked to
         * create a new {@code TaskOutputProcessor} for each execution of the
         * command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard output.
         *
         * @param stdOutProcessor the factory of {@link TaskOutputProcessor}
         *   processing the standard output of the associated Gradle command.
         *   This argument can be {@code null}, if there is nothing to do after
         *   lines are written to the standard output.
         */
        public void setSingleExecutionStdOutProcessor(@Nullable SingleExecutionOutputProcessor stdOutProcessor) {
            this.singleExecutionStdOutProcessor = stdOutProcessor;
        }

        /**
         * Sets a factory of {@link TaskOutputProcessor} processing the standard
         * error of the associated Gradle command. The factory is asked to
         * create a new {@code TaskOutputProcessor} for each execution of the
         * command.
         * An invocation of this method overwrites previous invocation of this
         * method.
         * <P>
         * You may set this property to {@code null}, if there is nothing to do
         * after lines are written to the standard error.
         *
         * @param stdErrProcessor factory of {@link TaskOutputProcessor}
         *   processing the standard error of the associated Gradle command.
         *   This argument can be {@code null}, if there is nothing to do after
         *   lines are written to the standard error.
         */
        public void setSingleExecutionStdErrProcessor(@Nullable SingleExecutionOutputProcessor stdErrProcessor) {
            this.singleExecutionStdErrProcessor = stdErrProcessor;
        }

        /**
         * Sets the custom code to be executed before and after the Gradle
         * command has been executed successfully. The custom code may use the
         * {@code Lookup} context used to start the associated Gradle command
         * and also the {@link org.netbeans.api.project.Project Project} instance.
         * <P>
         * You may set this property to {@code null}, if no such action
         * is needed. The default value for this property is {@code null}.
         *
         * @param contextAwareAction the custom code to be executed before and
         *   after the Gradle command has been executed successfully. This
         *   argument can be {@code null}, if no code needs to be executed.
         */
        public void setContextAwareAction(@Nullable ContextAwareCommandAction contextAwareAction) {
            this.contextAwareAction = contextAwareAction;
        }

        /**
         * Sets the custom code to be executed before and after the Gradle
         * command has been executed. The custom code may use the {@code Lookup}
         * context used to start the associated Gradle command and also the
         * {@link org.netbeans.api.project.Project Project} instance.
         * <P>
         * You may set this property to {@code null}, if no such action
         * is needed. The default value for this property is {@code null}.
         *
         * @param contextAwareFinalizer the custom code to be executed before and
         *   after the Gradle command has been executed. This argument can be
         *   {@code null}, if no code needs to be executed.
         */
        public void setContextAwareFinalizer(@Nullable ContextAwareCommandCompleteAction contextAwareFinalizer) {
            this.contextAwareFinalizer = contextAwareFinalizer;
        }

        /**
         * Sets a custom code (based on calling context) to verify if the
         * associated task can be executed by the given Gradle process. The
         * properties of Gradle available to check is defined in
         * {@link org.netbeans.gradle.project.api.modelquery.GradleTarget GradleTarget}.
         * <P>
         * You may set this property to {@code null}, if no such action
         * is needed (i.e., the task is to be executed for any version of Gradle).
         * The default value for this property is {@code null}.
         * <P>
         * If both this property and
         * {@link #setGradleTargetVerifier(GradleTargetVerifier) GradleTargetVerifier}
         * is set, then both of them must allow the command to run. The order in
         * which they are called is not defined but if the first one called
         * denies execution of the command, the second one will not be called.
         *
         * @param contextAwareGradleTargetVerifier the custom code to be used to
         *   verify if the associated task can be executed or not. This argument
         *   can be {@code null} if no such action is needed.
         *
         * @see org.netbeans.gradle.project.api.modelquery.GradleTarget
         * @see #setGradleTargetVerifier(GradleTargetVerifier)
         */
        public void setContextAwareGradleTargetVerifier(@Nullable ContextAwareGradleTargetVerifier contextAwareGradleTargetVerifier) {
            this.contextAwareGradleTargetVerifier = contextAwareGradleTargetVerifier;
        }

        /**
         * Sets a custom code to verify if the associated task can be executed
         * by the given Gradle process. The properties of Gradle available to check
         * is defined in {@link org.netbeans.gradle.project.api.modelquery.GradleTarget GradleTarget}.
         * <P>
         * You may set this property to {@code null}, if no such action
         * is needed (i.e., the task is to be executed for any version of Gradle).
         * The default value for this property is {@code null}.
         * <P>
         * If both this property and
         * {@link #setContextAwareGradleTargetVerifier(ContextAwareGradleTargetVerifier) ContextAwareGradleTargetVerifier}
         * is set, then both of them must allow the command to run. The order in
         * which they are called is not defined but if the first one called
         * denies execution of the command, the second one will not be called.
         *
         * @param gradleTargetVerifier the custom code to be used to verify
         *   if the associated task can be executed or not. This argument
         *   can be {@code null} if no such action is needed.
         *
         * @see org.netbeans.gradle.project.api.modelquery.GradleTarget
         */
        public void setGradleTargetVerifier(@Nullable GradleTargetVerifier gradleTargetVerifier) {
            this.gradleTargetVerifier = gradleTargetVerifier;
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
        @Nonnull
        public CustomCommandActions create() {
            return new CustomCommandActions(this);
        }
    }

    private final TaskKind taskKind;
    private final CommandCompleteListener commandCompleteListener;
    private final TaskOutputProcessor stdOutProcessor;
    private final TaskOutputProcessor stdErrProcessor;
    private final SingleExecutionOutputProcessor singleExecutionStdOutProcessor;
    private final SingleExecutionOutputProcessor singleExecutionStdErrProcessor;
    private final ContextAwareCommandAction contextAwareAction;
    private final ContextAwareCommandCompleteAction contextAwareFinalizer;
    private final GradleTargetVerifier gradleTargetVerifier;
    private final ContextAwareGradleTargetVerifier contextAwareGradleTargetVerifier;
    private final CommandExceptionHider commandExceptionHider;
    private final ContextAwareCommandArguments contextAwareCommandArguments;
    private final CancellationToken cancelToken;
    private final GradleCommandServiceFactory commandServiceFactory;

    private CustomCommandActions(Builder builder) {
        this.taskKind = builder.taskKind;
        this.commandCompleteListener = builder.commandCompleteListener;
        this.stdOutProcessor = builder.stdOutProcessor;
        this.stdErrProcessor = builder.stdErrProcessor;
        this.singleExecutionStdOutProcessor = builder.singleExecutionStdOutProcessor;
        this.singleExecutionStdErrProcessor = builder.singleExecutionStdErrProcessor;
        this.contextAwareAction = builder.contextAwareAction;
        this.contextAwareFinalizer = builder.contextAwareFinalizer;
        this.gradleTargetVerifier = builder.gradleTargetVerifier;
        this.contextAwareGradleTargetVerifier = builder.contextAwareGradleTargetVerifier;
        this.commandExceptionHider = builder.commandExceptionHider;
        this.contextAwareCommandArguments = builder.contextAwareCommandArguments;
        this.cancelToken = builder.cancelToken;
        this.commandServiceFactory = builder.commandServiceFactory;
    }

    /**
     * Returns the {@code GradleCommandServiceFactory} which might define a
     * service which lives during the execution of the associated Gradle command.
     * The service may also provide {@link TaskVariable task variables} as
     * a last chance to replace task variables in Gradle command templates.
     *
     * @return the {@code GradleCommandServiceFactory}
     *   which might define a service which lives during the execution of
     *   the associated Gradle command. This method may return {@code null}
     *   if no such service is required.
     */
    @Nullable
    public GradleCommandServiceFactory getCommandServiceFactory() {
        return commandServiceFactory;
    }

    /**
     * Returns the {@code CancellationToken} which signals cancellation if the
     * build is to be terminated without waiting to be completed.
     *
     * @return the {@code CancellationToken} which signals cancellation if the
     *   build is to be terminated without waiting to be completed. This
     *   method never returns {@code null}.
     */
    public CancellationToken getCancelToken() {
        return cancelToken;
    }

    /**
     * Returns the {@code ContextAwareCommandArguments} which is used
     * to provide additional {@link GradleCommandTemplate#getArguments() arguments}
     * for the Gradle command to be executed. The arguments will be appended
     * to the argument list after what is specified by
     * {@link GradleCommandTemplate}.
     * <P>
     * The additional arguments are processed the same way as if they were
     * specified in the associated {@code GradleCommandTemplate}.
     *
     * @return the {@code ContextAwareCommandArguments} which is used to provide
     *   additional arguments for the Gradle command to be executed. This method
     *   may return {@code null} if no additional arguments are needed.
     */
    public ContextAwareCommandArguments getContextAwareCommandArguments() {
        return contextAwareCommandArguments;
    }

    /**
     * Returns the check to be used if an exception thrown by the associated
     * Gradle command should be hidden or not (or {@code null} if no check is
     * needed). That is, if the specified exception hider returns {@code true}
     * (meaning that the exception must be hidden), then the exception will not
     * be displayed to the user (it will still be logged on {@code INFO} level).
     * <P>
     * Note that this check may also display the error to the user in other
     * forms (better describing what happened to the user).
     *
     * @return the check to be used if an exception thrown by the associated
     *   Gradle command should be hidden or not. This method may return
     *   {@code null}, if no exception is needed to be hidden.
     */
    @Nullable
    public CommandExceptionHider getCommandExceptionHider() {
        return commandExceptionHider;
    }

    /**
     * Returns a {@code CustomCommandActions} instance with the given
     * {@code TaskKind} and other properties set to their default values.
     * <P>
     * This is similar to {@code new Builder(taskKind).create()} but may return
     * cached instances which is more efficient.
     *
     * @param taskKind the kind of task affecting the output window handling of
     *   the executed task. This argument cannot be {@code null}.
     * @return a {@code CustomCommandActions} instance with the given
     *   {@code TaskKind} and other properties set to their default values. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public static CustomCommandActions simpleAction(@Nonnull TaskKind taskKind) {
        switch (Objects.requireNonNull(taskKind, "taskKind")) {
            case BUILD:
                return BUILD;
            case RUN:
                return RUN;
            case DEBUG:
                return DEBUG;
            case OTHER:
                return OTHER;
            default:
                LOGGER.log(Level.WARNING, "Missing case for {0}", taskKind);
                return new Builder(taskKind).create();
        }
    }

    /**
     * Returns the kind of tasks affecting the output window handling of the
     * executed task.
     *
     * @return the kind of tasks affecting the output window handling of the
     *   executed task. This method may never return {@code null}.
     */
    @Nonnull
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
    @Nullable
    @CheckForNull
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
    @Nullable
    @CheckForNull
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
    @Nullable
    @CheckForNull
    public TaskOutputProcessor getStdErrProcessor() {
        return stdErrProcessor;
    }

    /**
     * Returns the factory of {@link TaskOutputProcessor} processing the
     * standard output of the associated Gradle command or {@code null} if there
     * is nothing to do after lines are written to the standard output.
     *
     * @return the factory of {@link TaskOutputProcessor} processing the
     *   standard output of the associated Gradle command or {@code null} if
     *   there is nothing to do after lines are written to the standard output
     */
    @Nullable
    @CheckForNull
    public SingleExecutionOutputProcessor getSingleExecutionStdOutProcessor() {
        return singleExecutionStdOutProcessor;
    }

    /**
     * Returns the factory of {@link TaskOutputProcessor} processing the
     * standard error of the associated Gradle command or {@code null} if there
     * is nothing to do after lines are written to the standard error.
     *
     * @return the factory of {@link TaskOutputProcessor} processing the
     *   standard error of the associated Gradle command or {@code null} if
     *   there is nothing to do after lines are written to the standard error
     */
    @Nullable
    @CheckForNull
    public SingleExecutionOutputProcessor getSingleExecutionStdErrProcessor() {
        return singleExecutionStdErrProcessor;
    }

    /**
     * Returns the custom code to be executed before and after the Gradle
     * command has been executed successfully. The custom code may use the
     * {@code Lookup} context used to start the associated Gradle command and
     * also the {@link org.netbeans.api.project.Project Project} instance.
     *
     * @return the custom code to be executed before and after the Gradle
     *   command has been executed successfully. This method may return
     *   {@code null}, if no such action needs to be executed.
     */
    @Nullable
    @CheckForNull
    public ContextAwareCommandAction getContextAwareAction() {
        return contextAwareAction;
    }

    /**
     * Returns the custom code to be executed before and after the Gradle
     * command has been executed. The custom code may use the {@code Lookup}
     * context used to start the associated Gradle command and
     * also the {@link org.netbeans.api.project.Project Project} instance.
     *
     * @return the custom code to be executed before and after the Gradle
     *   command has been executed. This method may return {@code null}, if no
     *   such action needs to be executed.
     */
    @Nullable
    @CheckForNull
    public ContextAwareCommandCompleteAction getContextAwareFinalizer() {
        return contextAwareFinalizer;
    }

    /**
     * Returns the custom code (based on calling context) to verify if the
     * associated task can be executed by the given Gradle process. The
     * properties of Gradle available to check is defined in
     * {@link org.netbeans.gradle.project.api.modelquery.GradleTarget GradleTarget}.
     * <P>
     * If this property is {@code null}, the task is to be executed for any
     * version of Gradle.
     *
     * @return the custom code to verify if the associated task can be executed
     *   by the given Gradle process. This method may return {@code null}, if
     *   no such action needs to be executed.
     */
    @Nullable
    @CheckForNull
    public ContextAwareGradleTargetVerifier getContextAwareGradleTargetVerifier() {
        return contextAwareGradleTargetVerifier;
    }

    /**
     * Returns the custom code to verify if the associated task can be executed
     * by the given Gradle process. The properties of Gradle available to check
     * is defined in {@link org.netbeans.gradle.project.api.modelquery.GradleTarget GradleTarget}.
     * <P>
     * If this property is {@code null}, the task is to be executed for any
     * version of Gradle.
     *
     * @return the custom code to verify if the associated task can be executed
     *   by the given Gradle process.  This method may return {@code null}, if
     *   no such action needs to be executed.
     */
    @Nullable
    @CheckForNull
    public GradleTargetVerifier getGradleTargetVerifier() {
        return gradleTargetVerifier;
    }
}
