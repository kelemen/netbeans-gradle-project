package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim2.property.PropertySource;

/**
 * Defines a conversion (to and from) between property values and keys storable
 * in a configuration file.
 * <P>
 * The implementation of this interface are expected to be stateless and
 * completely thread-safe.
 *
 * @param <ValueKey> the type of the property key in a format saveable into a
 *   configuration file
 * @param <ValueType> the type of the final value of the property
 *
 * @see PropertyDef
 * @see PropertyKeyEncodingDef
 */
public interface PropertyValueDef<ValueKey, ValueType> {
    /**
     * Converts a property key representing this property in a configuration
     * file to the final value of the property. This conversion might use
     * information from external sources. For example, if the final type is a
     * {@code JavaPlatform} and the configuration key is simply a version number.
     * This method might need to rely on the platform list of NetBeans.
     *
     * @param valueKey the property key being converted. This argument can be
     *   {@code null}, which represents the default configuration (usually the
     *   configuration is not set in the configuration file).
     * @return the property tracking the final value of the property. The value
     *   of this property may change if the information used for the conversion
     *   might change without the configuration key changing. This method may
     *   never return {@code null}.
     */
    @Nonnull
    public PropertySource<ValueType> property(@Nullable ValueKey valueKey);

    /**
     * Converts a property value to a format saveable to a configuration file.
     *
     * @param value the property value to be converted. This argument can be
     *   {@code null} if the {@link #property(Object) property} method may
     *   convert anything to {@code null}.
     * @return the saveable property key. This method may return {@code null},
     *   if the property currently selected is the default value.
     */
    @Nullable
    public ValueKey getKeyFromValue(@Nullable ValueType value);
}
