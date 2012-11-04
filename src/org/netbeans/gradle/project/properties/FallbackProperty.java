package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public final class FallbackProperty<ValueType> implements MutableProperty<ValueType> {
    private final MutableProperty<ValueType> mainValue;
    private final MutableProperty<ValueType> defaultValue;

    public FallbackProperty(MutableProperty<ValueType> mainValue, MutableProperty<ValueType> defaultValue) {
        if (mainValue == null) throw new NullPointerException("mainValue");
        if (defaultValue == null) throw new NullPointerException("defaultValue");

        this.mainValue = mainValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public void setValueFromSource(PropertySource<? extends ValueType> source) {
        mainValue.setValueFromSource(source);
    }

    @Override
    public void setValue(ValueType value) {
        mainValue.setValue(value);
    }

    @Override
    public ValueType getValue() {
        return mainValue.isDefault()
                ? defaultValue.getValue()
                : mainValue.getValue();
    }

    @Override
    public boolean isDefault() {
        return mainValue.isDefault() && defaultValue.isDefault();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        if (listener == null) throw new NullPointerException("listener");

        mainValue.addChangeListener(listener);
        defaultValue.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        if (listener == null) throw new NullPointerException("listener");

        defaultValue.removeChangeListener(listener);
        mainValue.removeChangeListener(listener);
    }
}
