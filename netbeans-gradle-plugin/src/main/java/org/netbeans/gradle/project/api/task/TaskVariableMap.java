package org.netbeans.gradle.project.api.task;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a map for retrieving values for a given {@code TaskVariable}.
 * Instances of this map are recommended to be lazily constructed.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see GradleTaskVariableQuery
 */
public interface TaskVariableMap {
    /**
     * Returns the value which should replace the given variable or {@code null}
     * if this map does not knows the replacement string for the given variable.
     * <P>
     * This method is always called on a background thread, therefore may do
     * some I/O but know that this might slow than Gradle command execution as
     * this method might be called before each Gradle command (possibly multiple
     * times).
     *
     * @param variable the {@code TaskVariable} which is to be replaced. This
     *   argument cannot be {@code null}.
     * @return the value which should replace the given variable or {@code null}
     *   if this map does not knows the replacement string for the given
     *   variable. The return values must be the same (in terms of
     *   {@code equals}) if called multiple times with the same (in terms of
     *   {@code equals}) {@code TaskVariable}.
     */
    @Nullable
    public String tryGetValueForVariable(@Nonnull TaskVariable variable);
}
