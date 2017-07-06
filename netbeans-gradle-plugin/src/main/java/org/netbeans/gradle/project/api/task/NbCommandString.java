package org.netbeans.gradle.project.api.task;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Defines the command string NetBeans passes to
 * {@link org.netbeans.spi.project.ActionProvider ActionProvider} implementations.
 * <P>
 * An instance of this object can be retrieved from the context {@code Lookup}
 * anywhere where this plugin passes the context to a method.
 *
 * @see ContextAwareCommandAction
 * @see ContextAwareCommandArguments
 * @see ContextAwareCommandCompleteAction
 * @see ContextAwareGradleTargetVerifier
 */
public final class NbCommandString {
    private final String commandString;

    /**
     * Creates an {@code NbCommandString} with the specified command strings.
     *
     * @param commandString the command string which was (or maybe) passed to
     *   the {@link org.netbeans.spi.project.ActionProvider ActionProvider}.
     *   This argument cannot be {@code null}.
     */
    public NbCommandString(@Nonnull String commandString) {
        this.commandString = Objects.requireNonNull(commandString, "commandString");
    }

    /**
     * Returns the command string as specified at construction time. The command
     * string was (or maybe) passed to the
     * {@link org.netbeans.spi.project.ActionProvider ActionProvider}.
     *
     * @return the command string. This method never returns {@code null}.
     */
    @Nonnull
    public String getCommandString() {
        return commandString;
    }

    /**
     * {@inheritDoc }
     *
     * @return a hash code compatible with the {@code equals} method
     */
    @Override
    public int hashCode() {
        return 469 + commandString.hashCode();
    }

    /**
     * Returns {@code true}, if and only, if the specified object is an instance
     * of {@code NbCommandString} and has the same
     * {@link #getCommandString() command string}.
     *
     * @return {@code true} if this {@code NbCommandString} equals to the
     *   given object, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final NbCommandString other = (NbCommandString)obj;
        return this.commandString.equals(other.commandString);
    }

    /**
     * Returns the same value as {@link #getCommandString() getCommandString}.
     *
     * @return the command string. This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return commandString;
    }
}
