package org.netbeans.gradle.project.api.task;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.tasks.vars.EmptyTaskVarMap;

/**
 * Defines a running service which lives while an associated Gradle command
 * is being executed. This service may provide additional
 * {@link TaskVariable task variables} whose values has been determined when
 * this service was started.
 * <P>
 * The service will be stopped (i.e., its {@code close} method is called) after
 * the associated command terminated.
 *
 * @see GradleCommandServiceFactory
 */
public interface GradleCommandService extends AutoCloseable {
    /**
     * Defines a service which does nothing and provides no task variables.
     */
    public static final GradleCommandService NO_SERVICE = new GradleCommandService() {
        @Override
        public TaskVariableMap getTaskVariables() {
            return EmptyTaskVarMap.INSTANCE;
        }

        @Override
        public void close() throws IOException {
        }
    };

    /**
     * Returns the {@link TaskVariable task variables} whose values are provided
     * by this service. The variables provide a last chance to adjust the
     * task names and variables in the Gradle command template.
     *
     * @return the {@code TaskVariableMap} defining the task variables of this
     *   service. This method may never return {@code null};
     */
    @Nonnull
    public TaskVariableMap getTaskVariables();

    /**
     * Stops this service and disposes any resources this service may use.
     * <P>
     * This method is only called once, after the associated Gradle command
     * has terminated.
     *
     * @throws IOException thrown if there was an error while stopping this
     *   service.
     */
    @Override
    public void close() throws IOException;
}
