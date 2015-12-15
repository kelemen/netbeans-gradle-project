package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines the strategy to merge a value with its fallback value.
 * <P>
 * The implementation of this interface are expected to be stateless and
 * completely thread-safe.
 *
 * @param <ValueType> the type of the values to be merged
 *
 * @see PropertyDef
 */
public interface ValueMerger<ValueType> {
    /**
     * Merges the specified value with its fallback value.
     * <P>
     * A very common strategy is to fallback to the parent value when the
     * base value is {@code null}.
     *
     * @param child the base value to be merged with the fallback value. This
     *   argument might be {@code null} if the associated property might have a
     *   {@code null} value.
     * @param parent the reference to the fallback value. Only a reference is
     *   provided to avoid unnecessary computation when the parent value is not
     *   needed. This argument cannot be {@code null}.
     * @return the merged value. This method may return {@code null} depending
     *   on the implementation.
     */
    public ValueType mergeValues(@Nullable ValueType child, @Nonnull ValueReference<ValueType> parent);
}
