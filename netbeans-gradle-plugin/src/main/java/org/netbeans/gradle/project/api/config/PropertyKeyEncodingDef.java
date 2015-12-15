package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines the parser and encoder of a configuration tree. Conversions
 * are not allowed to use information from other sources than the configuration
 * tree.
 * <P>
 * The implementation of this interface are expected to be stateless and
 * completely thread-safe.
 *
 * @param <ValueKey> the type of the key the configuration tree is parsed to.
 *   This type is strongly recommended to have its {@code equals} and
 *   {@code hashCode} properly implemented.
 *
 * @see PropertyDef
 */
public interface PropertyKeyEncodingDef<ValueKey> {
    /**
     * Parses the given configuration tree into a more meaningful object.
     * This method is not expected to fail for any configuration tree. If this
     * method cannot understand the provided configuration, it must return
     * {@code null}. Also, for consistency, this method should return
     * {@code null} when the given configuration tree is empty.
     *
     * @param config the configuration tree to be parsed. This argument cannot
     *   be {@code null}.
     * @return the parsed object representing the given configuration. This
     *   method may return {@code null}, in which case the configuration is
     *   interpreted as default configuration. That is, returning {@code null}
     *   is indistinguishable from the case where the configuration tree does
     *   not exist.
     */
    @Nullable
    public ValueKey decode(@Nonnull ConfigTree config);

    /**
     * Converts the given object into a configuration tree. The configuration
     * tree must later be decodeable by the {@link #decode(ConfigTree) decode}
     * method.
     *
     * @param value the object to be encoded. This argument cannot be {@code null}.
     *   {@code null} values will always be encoded as an empty tree.
     * @return the given object converted into a configuration tree. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public ConfigTree encode(@Nonnull ValueKey value);
}
