package org.netbeans.gradle.project.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.standard.CommonProperties;

/**
 * Defines how to store and parse the value of a particular property.
 * <P>
 * The values of the properties are stored in a tree where each edge is
 * identified by a string and each node might store a string value. There is no
 * restriction on the name of the edges and the values stored in the nodes. In
 * most practical use-cases, you can think of edges as {@literal XML} tags and
 * values as their {@literal CDATA} content.
 * <P>
 * Properties are parsed in multiple steps:
 * <ol>
 *   <li>
 *     The value stored in the configuration tree is parsed into a user defined
 *     type. This transformation must be idempotent and therefore may not use
 *     information from other sources but the configuration tree. The purpose of
 *     this step is to cache the result of the parsed value and prevent
 *     unnecessary re-parsing and check if the value in the settings changed or
 *     not (based on a possibly custom equality comparison).
 *   </li>
 *   <li>
 *     The result of the previous step is transformed into the final value
 *     of the property. This transformation might include information from
 *     external sources, even from sources which might change over time
 *     (assuming the changes are trackable).
 *   </li>
 *   <li>
 *     The value of the property can optionally be merged with a parent property.
 *     This is usually used to merge the value of a custom profile with the
 *     value of the default profile.
 *   </li>
 * </ol>
 * <P>
 * Instances of {@code PropertyDef} are immutable (assuming the implementation
 * of its properties are immutable which is strongly recommended).
 * <P>
 * Instances of {@code PropertyDef} are created through its {@link Builder}.
 *
 * @param <ValueKey> the type of the user defined object the property parsed
 *   into the first step
 * @param <ValueType> the type of the final value of the property
 *
 * @see ConfigTree
 * @see ProjectSettingsProvider
 */
public final class PropertyDef<ValueKey, ValueType> {
    /**
     * The builder of {@code PropertyDef} used to create new {@link PropertyDef}
     * instances.
     *
     * @param <ValueKey> the type of the user defined object the property parsed
     *   into the first step
     * @param <ValueType> the type of the final value of the property
     */
    public static final class Builder<ValueKey, ValueType> {
        private final List<ConfigPath> configPaths;

        private PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
        private PropertyValueDef<ValueKey, ValueType> valueDef;
        private ValueMerger<ValueType> valueMerger;
        private EqualityComparator<? super ValueKey> valueKeyEquality;

        /**
         * Initializes this builder with the given root path in the configuration
         * tree. That is, the configuration trees in
         * {@link PropertyKeyEncodingDef PropertyKeyEncodingDef}
         * will be relative to this path.
         *
         * @param configPath the root path of the property to be created. This
         *   argument cannot be {@code null}.
         */
        public Builder(@Nonnull ConfigPath configPath) {
            this(Collections.singleton(configPath));
        }

        /**
         * Initializes this builder with the multiple root paths in the
         * configuration tree. That is, the root paths specify under which
         * subtrees will the property to be parsed found.
         * <P>
         * The {@link PropertyKeyEncodingDef PropertyKeyEncodingDef}
         * will be relative to the deepest common parent of all root paths. For
         * example, if {@code /root/parent/sub1} and {@code /root/parent/sub2}
         * is specified as root, the common parent will be {@code /root/parent}.
         *
         * @param configPaths the root paths of the property to be created. This
         *   argument cannot be {@code null} and cannot contain {@code null}
         *   elements.
         */
        public Builder(@Nonnull Collection<ConfigPath> configPaths) {
            this.configPaths = ExceptionHelper.checkNotNullElements(CollectionsEx.readOnlyCopy(configPaths), "configPaths");
            this.keyEncodingDef = NoOpKeyEncodingDef.getInstance();
            this.valueDef = NoOpValueDef.getInstance();
            this.valueMerger = CommonProperties.getParentIfNullValueMerger();
            this.valueKeyEquality = Equality.naturalEquality();
        }

        /**
         * Sets the parser used to parse the value found in the configuration
         * tree to a more meaningful object. This can be an identity
         * transformation, however for efficency, it is usually recommended to
         * use a more specific custom type.
         * <P>
         * The default implementation will always return {@code null} for any
         * configuration tree and will transform any value to an empty to tree.
         *
         * @param keyEncodingDef the parser used to parse the value found in the
         *   configuration tree to a more meaningful object. This argument
         *   cannot be {@code null}.
         */
        public void setKeyEncodingDef(@Nonnull PropertyKeyEncodingDef<ValueKey> keyEncodingDef) {
            this.keyEncodingDef = Objects.requireNonNull(keyEncodingDef, "keyEncodingDef");
        }

        /**
         * Sets the parser used to transform the value created by the
         * {@link PropertyKeyEncodingDef} to the final value.
         * <P>
         * The default implementation always transforms everything to
         * {@code null} (in both direction).
         *
         * @param valueDef the parser used to transform the value created by the
         *   {@code PropertyKeyEncodingDef} to the final value. This argument
         *   cannot be {@code null}.
         */
        public void setValueDef(@Nonnull PropertyValueDef<ValueKey, ValueType> valueDef) {
            this.valueDef = Objects.requireNonNull(valueDef, "valueDef");
        }

        /**
         * Sets the {@link ValueMerger} defining the way of combining the value
         * of this property with its fallback property. Which is usually, how to
         * fall back to the fallback value.
         * <P>
         * The default implementation simply returns the parent value if the
         * current value is {@code null}.
         *
         * @param valueMerger the {@link ValueMerger} defining the way of
         *   combining the value of this property with its fallback property.
         *   This argument cannot be {@code null}.
         */
        public void setValueMerger(@Nonnull ValueMerger<ValueType> valueMerger) {
            this.valueMerger = Objects.requireNonNull(valueMerger, "valueMerger");
        }

        /**
         * Sets the equality definition of keys provided by the specified
         * {@link PropertyKeyEncodingDef}.
         * <P>
         * The default implementation compares objects by their {@code equals}
         * method (which is usually appropriate).
         *
         * @param valueKeyEquality the equality definition of keys provided by
         *   the specified {@link PropertyKeyEncodingDef}. This argument cannot
         *   be {@code null}.
         */
        public void setValueKeyEquality(@Nonnull EqualityComparator<? super ValueKey> valueKeyEquality) {
            this.valueKeyEquality = Objects.requireNonNull(valueKeyEquality, "valueKeyEquality");
        }

        /**
         * Creates a new immutable instance of {@code PropertyDef} based on the
         * properties set before calling this method.
         *
         * @return a new immutable instance of {@code PropertyDef} based on the
         *   properties set before calling this method. This method never
         *   returns {@code null}.
         */
        @Nonnull
        public PropertyDef<ValueKey, ValueType> create() {
            return new PropertyDef<>(this);
        }
    }

    private final List<ConfigPath> configPaths;
    private final PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
    private final PropertyValueDef<ValueKey, ValueType> valueDef;
    private final ValueMerger<ValueType> valueMerger;
    private final EqualityComparator<? super ValueKey> valueKeyEquality;

    private PropertyDef(Builder<ValueKey, ValueType> builder) {
        this.keyEncodingDef = builder.keyEncodingDef;
        this.valueDef = builder.valueDef;
        this.valueMerger = builder.valueMerger;
        this.valueKeyEquality = builder.valueKeyEquality;
        this.configPaths = builder.configPaths;
    }

    private PropertyDef(PropertyDef<ValueKey, ValueType> baseDef, List<ConfigPath> newConfigPaths) {
        this.keyEncodingDef = baseDef.keyEncodingDef;
        this.valueDef = baseDef.valueDef;
        this.valueMerger = baseDef.valueMerger;
        this.valueKeyEquality = baseDef.valueKeyEquality;
        this.configPaths = newConfigPaths;
    }

    /**
     * Returns a new {@code PropertyDef} which has the same properties as this
     * {@code PropertyDef} except that all of its {@link #getConfigPaths() configuration roots}
     * will start with the given keys (as done by
     * {@link ConfigPath#withParentPath(String[]) ConfigPath.withParentPath}).
     *
     * @param parentKeys the keys prepended to all the configuration roots. This
     *   argument cannot be {@code null} and none of the keys can be {@code null}.
     * @return the new {@code PropertyDef} which has the same properties as this
     *   {@code PropertyDef} except that all of its configuration roots
     *   will start with the given keys. This method never returns {@code null}.
     */
    @Nonnull
    public PropertyDef<ValueKey, ValueType> withParentConfigPath(@Nonnull String... parentKeys) {
        List<ConfigPath> newConfigPaths = new ArrayList<>(configPaths.size());
        for (ConfigPath path: configPaths) {
            newConfigPaths.add(path.withParentPath(parentKeys));
        }
        return new PropertyDef<>(this, Collections.unmodifiableList(newConfigPaths));
    }

    /**
     * Returns the configuration roots from which this property reads its value.
     * <P>
     * The {@link PropertyKeyEncodingDef PropertyKeyEncodingDef}
     * will be relative to the deepest common parent of all root paths. For
     * example, if {@code /root/parent/sub1} and {@code /root/parent/sub2}
     * is specified as root, the common parent will be {@code /root/parent}.
     *
     * @return the configuration roots from which this property reads its value.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public List<ConfigPath> getConfigPaths() {
        return configPaths;
    }

    /**
     * Returns the parser used to parse the value found in the configuration
     * tree to a more meaningful object. The result of this parsing will be
     * converted by the {@link PropertyValueDef} to its final value.
     *
     * @return the parser used to parse the value found in the configuration
     *   tree to a more meaningful object. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public PropertyKeyEncodingDef<ValueKey> getKeyEncodingDef() {
        return keyEncodingDef;
    }

    /**
     * Returns the parser used to transform the value created by the
     * {@link PropertyKeyEncodingDef} to the final value.
     *
     * @return the parser used to transform the value created by the
     *   {@link PropertyKeyEncodingDef} to the final value. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public PropertyValueDef<ValueKey, ValueType> getValueDef() {
        return valueDef;
    }

    /**
     * Returns the {@link ValueMerger} defining the way of combining the value
     * of this property with its fallback property. Which is usually, how to
     * fall back to the fallback value.
     *
     * @return the {@link ValueMerger} defining the way of combining the value
     *   of this property with its fallback property. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public ValueMerger<ValueType> getValueMerger() {
        return valueMerger;
    }

    /**
     * Returns the equality definition of keys provided by the specified
     * {@link PropertyKeyEncodingDef}. This can be used to determine of the
     * value of the property has changed or not and so, should we update the
     * properties.
     *
     * @return the equality definition of keys provided by the specified
     *   {@link PropertyKeyEncodingDef}. This method never returns {@code null}.
     */
    @Nonnull
    public EqualityComparator<? super ValueKey> getValueKeyEquality() {
        return valueKeyEquality;
    }

    private static final class NoOpKeyEncodingDef<ValueKey>
    implements
            PropertyKeyEncodingDef<ValueKey> {

        private static final PropertyKeyEncodingDef<?> INSTANCE = new NoOpKeyEncodingDef<>();

        @SuppressWarnings("unchecked")
        public static <ValueKey> PropertyKeyEncodingDef<ValueKey> getInstance() {
            return (PropertyKeyEncodingDef<ValueKey>)INSTANCE;
        }

        @Override
        public ValueKey decode(ConfigTree config) {
            return null;
        }

        @Override
        public ConfigTree encode(ValueKey value) {
            return ConfigTree.EMPTY;
        }
    }

    private static final class NoOpValueDef<ValueKey, ValueType>
    implements
            PropertyValueDef<ValueKey, ValueType> {

        private static final PropertyValueDef<?, ?> INSTANCE = new NoOpValueDef<>();

        @SuppressWarnings("unchecked")
        public static <ValueKey, ValueType> PropertyValueDef<ValueKey, ValueType> getInstance() {
            return (PropertyValueDef<ValueKey, ValueType>)INSTANCE;
        }

        @Override
        public PropertySource<ValueType> property(ValueKey valueKey) {
            return PropertyFactory.constSource(null);
        }

        @Override
        public ValueKey getKeyFromValue(ValueType value) {
            return null;
        }
    }
}
