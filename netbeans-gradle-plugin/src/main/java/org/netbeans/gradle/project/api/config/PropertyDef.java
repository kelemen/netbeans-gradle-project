package org.netbeans.gradle.project.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.collections.Equality;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.standard.CommonProperties;

public final class PropertyDef<ValueKey, ValueType> {
    public static final class Builder<ValueKey, ValueType> {
        private final List<ConfigPath> configPaths;

        private PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
        private PropertyValueDef<ValueKey, ValueType> valueDef;
        private ValueMerger<ValueType> valueMerger;
        private EqualityComparator<? super ValueKey> valueKeyEquality;

        public Builder(@Nonnull ConfigPath configPath) {
            this(Collections.singleton(configPath));
        }

        public Builder(@Nonnull Collection<ConfigPath> configPaths) {
            this.configPaths = CollectionsEx.readOnlyCopy(configPaths);
            ExceptionHelper.checkNotNullElements(this.configPaths, "configPaths");

            this.keyEncodingDef = NoOpKeyEncodingDef.getInstance();
            this.valueDef = NoOpValueDef.getInstance();
            this.valueMerger = CommonProperties.getParentIfNullValueMerger();
            this.valueKeyEquality = Equality.naturalEquality();
        }

        public void setKeyEncodingDef(@Nonnull PropertyKeyEncodingDef<ValueKey> keyEncodingDef) {
            ExceptionHelper.checkNotNullArgument(keyEncodingDef, "keyEncodingDef");
            this.keyEncodingDef = keyEncodingDef;
        }

        public void setValueDef(@Nonnull PropertyValueDef<ValueKey, ValueType> valueDef) {
            ExceptionHelper.checkNotNullArgument(valueDef, "valueDef");
            this.valueDef = valueDef;
        }

        public void setValueMerger(@Nonnull ValueMerger<ValueType> valueMerger) {
            ExceptionHelper.checkNotNullArgument(valueMerger, "valueMerger");
            this.valueMerger = valueMerger;
        }

        public void setValueKeyEquality(EqualityComparator<? super ValueKey> valueKeyEquality) {
            ExceptionHelper.checkNotNullArgument(valueKeyEquality, "valueKeyEquality");
            this.valueKeyEquality = valueKeyEquality;
        }

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

    private PropertyDef(PropertyDef baseDef, List<ConfigPath> newConfigPaths) {
        this.keyEncodingDef = baseDef.keyEncodingDef;
        this.valueDef = baseDef.valueDef;
        this.valueMerger = baseDef.valueMerger;
        this.valueKeyEquality = baseDef.valueKeyEquality;
        this.configPaths = newConfigPaths;
    }

    public PropertyDef<ValueKey, ValueType> withParentConfigPath(String... parentKeys) {
        List<ConfigPath> newConfigPaths = new ArrayList<>(configPaths.size());
        for (ConfigPath path: configPaths) {
            newConfigPaths.add(path.withParentPath(parentKeys));
        }
        return new PropertyDef<>(this, Collections.unmodifiableList(newConfigPaths));
    }

    @Nonnull
    public List<ConfigPath> getConfigPaths() {
        return configPaths;
    }

    @Nonnull
    public PropertyKeyEncodingDef<ValueKey> getKeyEncodingDef() {
        return keyEncodingDef;
    }

    @Nonnull
    public PropertyValueDef<ValueKey, ValueType> getValueDef() {
        return valueDef;
    }

    @Nonnull
    public ValueMerger<ValueType> getValueMerger() {
        return valueMerger;
    }

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
