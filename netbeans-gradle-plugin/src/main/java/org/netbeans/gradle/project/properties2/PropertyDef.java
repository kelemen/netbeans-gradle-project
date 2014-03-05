package org.netbeans.gradle.project.properties2;

import javax.annotation.Nonnull;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class PropertyDef<ValueKey, ValueType> {
    public static final class Builder<ValueKey, ValueType> {
        private PropertyXmlDef<ValueKey> xmlDef;
        private PropertyValueDef<ValueKey, ValueType> valueDef;
        private ValueMerger<ValueType> valueMerger;

        public Builder() {
            this.xmlDef = NoOpXmlDef.getInstance();
            this.valueDef = NoOpValueDef.getInstance();
            this.valueMerger = NoOpValueMerger.getInstance();
        }

        public void setXmlDef(@Nonnull PropertyXmlDef<ValueKey> xmlDef) {
            ExceptionHelper.checkNotNullArgument(xmlDef, "xmlDef");
            this.xmlDef = xmlDef;
        }

        public void setValueDef(@Nonnull PropertyValueDef<ValueKey, ValueType> valueDef) {
            ExceptionHelper.checkNotNullArgument(valueDef, "valueDef");
            this.valueDef = valueDef;
        }

        public void setValueMerger(@Nonnull ValueMerger<ValueType> valueMerger) {
            ExceptionHelper.checkNotNullArgument(valueMerger, "valueMerger");
            this.valueMerger = valueMerger;
        }

        public PropertyDef<ValueKey, ValueType> create() {
            return new PropertyDef<>(this);
        }
    }

    private final PropertyXmlDef<ValueKey> xmlDef;
    private final PropertyValueDef<ValueKey, ValueType> valueDef;
    private final ValueMerger<ValueType> valueMerger;

    private PropertyDef(Builder<ValueKey, ValueType> builder) {
        this.xmlDef = builder.xmlDef;
        this.valueDef = builder.valueDef;
        this.valueMerger = builder.valueMerger;
    }

    @Nonnull
    public PropertyXmlDef<ValueKey> getXmlDef() {
        return xmlDef;
    }

    @Nonnull
    public PropertyValueDef<ValueKey, ValueType> getValueDef() {
        return valueDef;
    }

    @Nonnull
    public ValueMerger<ValueType> getValueMerger() {
        return valueMerger;
    }

    private static final class NoOpXmlDef<ValueKey>
    implements
            PropertyXmlDef<ValueKey> {

        private static final PropertyXmlDef<?> INSTANCE = new NoOpXmlDef<>();

        @SuppressWarnings("unchecked")
        public static <ValueKey> PropertyXmlDef<ValueKey> getInstance() {
            return (PropertyXmlDef<ValueKey>)INSTANCE;
        }

        @Override
        public ValueKey loadFromXml(Element node) {
            return null;
        }

        @Override
        public void addToXml(Element parent, ValueKey value) {
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
