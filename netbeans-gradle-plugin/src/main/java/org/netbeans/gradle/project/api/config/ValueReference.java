package org.netbeans.gradle.project.api.config;

/**
 * Defines a reference to an object. This is usually useful to lazily created
 * values.
 *
 * @param <ValueType> the type of the value referenced
 *
 * @see ValueMerger
 */
public interface ValueReference<ValueType> {
    /**
     * Returns the (possibly) lazily created value. This method must return
     * the same object when called multiple times.
     *
     * @return the (possibly) lazily created value. This method may return
     *   {@code null} depending on the implementation.
     */
    public ValueType getValue();
}
