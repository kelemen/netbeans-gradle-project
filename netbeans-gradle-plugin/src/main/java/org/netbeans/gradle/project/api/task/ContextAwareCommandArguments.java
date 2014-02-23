package org.netbeans.gradle.project.api.task;

import java.util.List;
import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Defines additional arguments to be added to the Gradle command to be
 * executed. The additional arguments are processed the same way as if they were
 * specified in the associated {@code GradleCommandTemplate}.
 * <P>
 * The method of this interface is called from a background thread (not the Event Dispatch Thread).
 *
 * @see CustomCommandActions
 */
public interface ContextAwareCommandArguments {
    /**
     * Returns the additional arguments to be added to the Gradle command to be
     * executed. The additional arguments are processed the same way as if they
     * were specified in the associated {@code GradleCommandTemplate}.
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
     * @return the additional arguments to be added to the Gradle command to be
     *   executed. This method may never return {@code null}.
     *
     * @see NbCommandString
     */
    @Nonnull
    public List<String> getCommandArguments(
            @Nonnull Project project,
            @Nonnull Lookup commandContext);
}
