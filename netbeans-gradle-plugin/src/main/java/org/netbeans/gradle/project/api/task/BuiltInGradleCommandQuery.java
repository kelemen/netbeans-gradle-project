package org.netbeans.gradle.project.api.task;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.netbeans.gradle.project.api.config.ProfileDef;

/**
 * Defines a query which defines the commands for the
 * {@link org.netbeans.spi.project.ActionProvider} of the project. A built-in
 * command for Gradle projects are made of a Gradle command to be executed
 * and a custom task to be executed after the command completes.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup()
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
    @Nonnull
    public Set<String> getSupportedCommands();

    /**
     * Returns the display name of the command, if know by the extension.
     * The following commands are known by Gradle Support and the extension may
     * choose to return {@code null} for them even if it
     * {@link #getSupportedCommands() supports} the command:
     * <ul>
     *  <li>{@code ActionProvider.COMMAND_BUILD}</li>
     *  <li>{@code ActionProvider.COMMAND_TEST}</li>
     *  <li>{@code ActionProvider.COMMAND_CLEAN}</li>
     *  <li>{@code ActionProvider.COMMAND_RUN}</li>
     *  <li>{@code ActionProvider.COMMAND_DEBUG}</li>
     *  <li>{@code ActionProvider.COMMAND_REBUILD}</li>
     *  <li>{@code ActionProvider.COMMAND_TEST_SINGLE}</li>
     *  <li>{@code ActionProvider.COMMAND_DEBUG_TEST_SINGLE}</li>
     *  <li>{@code ActionProvider.COMMAND_RUN_SINGLE}</li>
     *  <li>{@code ActionProvider.COMMAND_DEBUG_SINGLE}</li>
     *  <li>{@code JavaProjectConstants.COMMAND_JAVADOC}</li>
     *  <li>{@code JavaProjectConstants.COMMAND_DEBUG_FIX}</li>
     *  <li>{@code SingleMethod.COMMAND_RUN_SINGLE_METHOD}</li>
     *  <li>{@code SingleMethod.COMMAND_DEBUG_SINGLE_METHOD}</li>
     * </ul>
     *
     * @param command the command whose display name is to be returned. This
     *   argument cannot be {@code null}.
     * @return the display name of the command or {@code null} if this query
     *   does not know the display name for the specified command
     */
    @Nullable
    @CheckForNull
    public String tryGetDisplayNameOfCommand(@Nonnull String command);

    /**
     * Defines the default Gradle command to run when the built-in command is to
     * be executed. This command might be reconfigured by users, so there is no
     * guarantee that this command will actually be executed.
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
     *
     * @return the default Gradle command to run when the built-in command is to
     *   be executed. This method may return {@code null} in the case there are
     *   no Gradle associated Gradle command with the specified command or
     *   profile.
     */
    @Nullable
    @CheckForNull
    public GradleCommandTemplate tryGetDefaultGradleCommand(
            @Nullable ProfileDef profileDef,
            @Nonnull String command);

    /**
     * Returns the custom tasks associated with the specified built-in command.
     * The tasks returned by this method cannot be reconfigured by users.
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
     * @return the custom tasks associated with the specified built-in command.
     *   This method may return {@code null} if the associated extension does
     *   not define a Gradle command for the specified profile and command.
     */
    @Nullable
    @CheckForNull
    public CustomCommandActions tryGetCommandDefs(
            @Nullable ProfileDef profileDef,
            @Nonnull String command);
}
