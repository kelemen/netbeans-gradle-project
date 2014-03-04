package org.netbeans.gradle.project.properties;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySourceProxy;
import org.jtrim.utils.ExceptionHelper;

public final class DefaultMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private static final Logger LOGGER = Logger.getLogger(DefaultMutableProperty.class.getName());

    private final boolean allowNulls;
    private volatile PropertySource<? extends ValueType> valueSource;
    private final PropertySourceProxy<ValueType> valueSourceRef;

    public DefaultMutableProperty(ValueType value, boolean defaultValue, boolean allowNulls) {
        this(asSource(value, defaultValue, allowNulls), allowNulls);
    }

    public DefaultMutableProperty(
            final PropertySource<? extends ValueType> initialValue,
            boolean allowNulls) {
        ExceptionHelper.checkNotNullArgument(initialValue, "initialValue");

        this.allowNulls = allowNulls;
        this.valueSource = initialValue;
        this.valueSourceRef = PropertyFactory.proxySource(initialValue);
    }

    private static <ValueType> PropertySource<? extends ValueType> asSource(
            ValueType value, boolean defaultValue, boolean allowNulls) {
        return new ConstPropertySource<>(
                allowNulls ? value : checkNull(value),
                defaultValue);
    }

    private static <T> T checkNull(T value) {
        ExceptionHelper.checkNotNullArgument(value, "value");
        return value;
    }

    @Override
    public void setValueFromSource(PropertySource<? extends ValueType> source) {
        valueSourceRef.replaceSource(source);
    }

    @Override
    public void setValue(ValueType value) {
        if (!allowNulls) {
            ExceptionHelper.checkNotNullArgument(value, "value");
        }

        setValueFromSource(new ConstPropertySource<>(value, false));
    }

    @Override
    public ValueType getValue() {
        ValueType value = valueSource.getValue();
        if (value == null && !allowNulls) {
            String message = "The value of the property is null but null values are not permitted.";
            LOGGER.log(Level.SEVERE, message, new Exception(message));
        }
        return value;
    }

    @Override
    public boolean isDefault() {
        return valueSource.isDefault();
    }

    @Override
    public ListenerRef addChangeListener(final Runnable listener) {
        return valueSourceRef.addChangeListener(listener);
    }
}
