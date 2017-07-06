package org.netbeans.gradle.project.api.task;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.jtrim2.cancel.CancellationToken;

/**
 * Define a service which is active during the execution of a Gradle command.
 * <P>
 * The service may provide {@link TaskVariable task variables} whose values are
 * only available after the service has been started.
 * <P>
 * The {@code GradleCommandServiceFactory} must be set for the {@link CustomCommandActions custom actions}
 * of the command to be executed.
 *
 * @see CustomCommandActions
 */
public interface GradleCommandServiceFactory {
    /**
     * A service which does nothing and has no task variables.
     */
    public static final GradleCommandServiceFactory NO_SERVICE = new GradleCommandServiceFactory() {
        @Override
        public boolean isServiceTaskVariable(TaskVariable variable) {
            return false;
        }

        @Override
        public GradleCommandService startService(CancellationToken cancelToken, GradleCommandContext context) throws IOException {
            return GradleCommandService.NO_SERVICE;
        }
    };

    /**
     * Determines if the passed task variable will be defined by the started
     * started service or not. This method is called to determine if the user
     * must be queried for an undefined variable or not.
     *
     * @param variable the task variable to be checked if it is definied by
     *   the started service. This argument cannot be {@code null}.
     * @return {@code true} if the started task will provide value for the
     *   specified task variable, {@code false} if not
     */
    public boolean isServiceTaskVariable(@Nonnull TaskVariable variable);

    /**
     * Starts the service which will live until the execution of the associated
     * Gradle command. The service is only started right before command execution,
     * that is when there are no more blocking command to be executed and it is
     * determined that the command can be executed.
     *
     * @param cancelToken the {@code CancellationToken} which will signal
     *   cancellation if the user canceled the execution of the associated Gradle
     *   command. This argument cannot be {@code null}.
     * @param context the context in which the command is being executed,
     *   minimally including the project associated with the command. This
     *   argument cannot be {@code null}.
     * @return the newly started service which will be stopped after the
     *   associated Gradle command has terminated. This method may never return
     *   {@code null}.
     *
     * @throws IOException thrown if there was an error starting the service.
     *   The execution of the associated Gradle command will also fail if this
     *   method throws an exception.
     */
    @Nonnull
    public GradleCommandService startService(
            @Nonnull CancellationToken cancelToken,
            @Nonnull GradleCommandContext context) throws IOException;
}
