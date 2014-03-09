package org.netbeans.gradle.project.properties2;

import javax.annotation.Nonnull;
import org.jtrim.collections.Equality;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class PropertyDef<ValueKey, ValueType> {
    public static final class Builder<ValueKey, ValueType> {
        private PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
        private PropertyValueDef<ValueKey, ValueType> valueDef;
        private ValueMerger<ValueType> valueMerger;
        private EqualityComparator<? super ValueKey> valueKeyEquality;

        public Builder() {
            this.keyEncodingDef = NoOpKeyEncodingDef.getInstance();
            this.valueDef = NoOpValueDef.getInstance();
            this.valueMerger = NoOpValueMerger.getInstance();
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

    private final PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
    private final PropertyValueDef<ValueKey, ValueType> valueDef;
    private final ValueMerger<ValueType> valueMerger;
    private final EqualityComparator<? super ValueKey> valueKeyEquality;

    private PropertyDef(Builder<ValueKey, ValueType> builder) {
        this.keyEncodingDef = builder.keyEncodingDef;
        this.valueDef = builder.valueDef;
        this.valueMerger = builder.valueMerger;
        this.valueKeyEquality = builder.valueKeyEquality;
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

    private static final class NoOpValueMerger<ValueType>
    implements
            ValueMerger<ValueType> {

        private static final ValueMerger<?> INSTANCE = new NoOpValueMerger<>();

        @SuppressWarnings("unchecked")
        public static <ValueType> ValueMerger<ValueType> getInstance() {
            return (ValueMerger<ValueType>)INSTANCE;
        }

        @Override
        public ValueType mergeValues(ValueType child, ValueReference<ValueType> parent) {
            return child != null ? child : parent.getValue();
        }
    }
}
