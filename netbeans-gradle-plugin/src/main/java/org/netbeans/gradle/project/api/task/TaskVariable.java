package org.netbeans.gradle.project.api.task;

import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Defines a variable which might be replaced with a given string in Gradle
 * commands. Variables in strings take the form "${variable-name}" and their
 * name is case-sensitive. Variable names might only contain: alphanumerical
 * characters (English alphabet), dash ('-'), underscore ('_') and dot ('.')
 * and they must contain at least a single character.
 * <P>
 * The {@code equals} and {@code hashCode} methods of {@code TaskVariable} are
 * defined so that two {@code TaskVariable} instance equals, if and only, if
 * their {@link #getVariableName() name} equals (case-sensitive).
 * <P>
 * Instances of {@code TaskVariable} are immutable and as such are safe to be
 * accessed concurrently without any synchronization.
 *
 * @see GradleTaskVariableQuery
 */
public final class TaskVariable {
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9\\-_\\.]+");

    private final String variableName;

    /**
     * Creates a new {@code TaskVariable} with the given
     * {@link #getVariableName() name}.
     *
     * @param variableName the name of the variable to be replaced in strings.
     *   The replacement constant will be "${variableName}". This argument
     *   cannot be {@code null}, cannot be empty and may only contain characters
     *   {@link #isValidVariableName(String) allowed for task variables}.
     *
     * @throws NullPointerException if the passed variable name is {@code null}
     * @throws IllegalArgumentException thrown if the variable name is invalid
     *
     * @see #isValidVariableName(String)
     */
    public TaskVariable(@Nonnull String variableName) {
        Objects.requireNonNull(variableName, "variableName");

        if (!VARIABLE_NAME_PATTERN.matcher(variableName).matches()) {
            throw new IllegalArgumentException("Variable name contains an invalid character: " + variableName);
        }

        this.variableName = variableName;
    }

    /**
     * Checks if the given variable name is appropriate to use or not. The name
     * is appropriate, if and only, if they only contain alphanumerical
     * characters (English alphabet), dash ('-'), underscore ('_') or dot ('.')
     * and they must contain at least a single character.
     * <P>
     * If this method returns {@code true}, then calling the constructor of
     * {@code TaskVariable} with the given name will not throw an exception. If
     * this method returns {@code false}, then calling the constructor of
     * {@code TaskVariable} with the given name is guaranteed to throw an
     * IllegalArgumentException.
     *
     * @param variableName the name of the variable to be verified. This
     *   argument cannot be {@code null}.
     * @return {@code true} if this variable name is valid to use as a
     *   {@code TaskVariable}, {@code false} otherwise
     *
     * @throws NullPointerException if the passed variable name is {@code null}
     */
    public static boolean isValidVariableName(@Nonnull String variableName) {
        Objects.requireNonNull(variableName, "variableName");
        return VARIABLE_NAME_PATTERN.matcher(variableName).matches();
    }

    /**
     * Returns the name of the variable as specified at construction time.
     *
     * @return the name of the variable as specified at construction time.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns the variable name as it needs to appear in strings to be
     * replaced. This method simply returns:
     * <P>
     * {@code "${" + getVariableName() + "}"}.
     *
     * @return the variable name as it needs to appear in strings to be
     *   replaced. This method never returns {@code null}.
     */
    @Nonnull
    public String getScriptReplaceConstant() {
        return "${" + variableName + "}";
    }

    /**
     * {@inheritDoc }
     *
     * @return a hash code compatible with the {@code equals} method
     */
    @Override
    public int hashCode() {
        return 85 + variableName.hashCode();
    }

    /**
     * Checks if the specified object is a {@code TaskVariable} and has the
     * same {@link #getVariableName() name} as this {@code TaskVariable} or not.
     * <P>
     * This method will always return {@code false} if the specified object is
     * not an instance of {@code TaskVariable}.
     *
     * @param obj the object to be compared to this {@code TaskVariable}. This
     *   argument can be {@code null}, in which case the return value is
     *   {@code false}.
     * @return {@code true} if the specified object is a {@code TaskVariable}
     *   and has the same {@link #getVariableName() name} as this
     *   {@code TaskVariable}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final TaskVariable other = (TaskVariable)obj;
        return this.variableName.equals(other.variableName);
    }
}
