package org.netbeans.gradle.project.api.task;

import java.util.Set;
import org.netbeans.gradle.project.api.config.ProfileDef;

/**
 * Defines a query which defines the commands for the
 * {@link org.netbeans.spi.project.ActionProvider} of the project. A built-in
 * command for Gradle projects are made of a Gradle command to be executed
 * and a custom task to be executed after the command completes.
 * <P>
 * Instances of this interface are expected to be found on the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension#getExtensionLookup() lookup of the extension}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtension#getExtensionLookup()
 */
public interface BuiltInGradleCommandQuery {
    /**
     * Returns the set of commands supported by the associated extension. This
     * method needs to return the same string as defined by the
     * {@link org.netbeans.spi.project.ActionProvider#getSupportedActions()}.
     * method.
     *
     * @return the set of commands supported by the associated extension. This
     *   method may never return {@code null}. Although it is allowed to return
     *   an empty set, it is recommended not to implement
     *   {@code BuiltInGradleCommandQuery} in this case.
     */
    public Set<String> getSupportedCommands();

    /**
     * Returns the Gradle command to be associated with the specified profile
     * and built-in command or {@code null} if the associated extension does not
     * define a command.
     * <P>
     * This method must accept any profile or command even if
     * {@link #getSupportedCommands()} does not contain the specified command.
     * This method should return {@code null} for such case.
     *
     * @param profileDef the profile to which the command is requested. This
     *   argument can be {@code null} if the command is requested for the
     *   default profile.
     * @param command the command as defined by the
     *   {@link org.netbeans.spi.project.ActionProvider} interface. This
     *   argument cannot be {@code null}.
     * @return the Gradle command to be associated with the specified profile
     *   and built-in command. This method may return {@code null} if the
     *   associated extension does not define a Gradle command for the specified
     *   profile and command.
     */
    public BuiltInGradleCommand tryGetCommand(ProfileDef profileDef, String command);
}
