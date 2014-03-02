package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;

public final class FallbackProperty<ValueType> implements MutableProperty<ValueType> {
    private final MutableProperty<ValueType> mainValue;
    private final MutableProperty<ValueType> defaultValue;

    public FallbackProperty(MutableProperty<ValueType> mainValue, MutableProperty<ValueType> defaultValue) {
        ExceptionHelper.checkNotNullArgument(mainValue, "mainValue");
        ExceptionHelper.checkNotNullArgument(defaultValue, "defaultValue");

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
        // "mainValue.isDefault() && defaultValue.isDefault()" would be
        // more correct but this method is only used for FallbackProperty
        // instances by the ProjectPropertiesPanel and for the
        // ProjectPropertiesPanel this return value is better.
        return mainValue.isDefault();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        mainValue.addChangeListener(listener);
        defaultValue.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        defaultValue.removeChangeListener(listener);
        mainValue.removeChangeListener(listener);
    }
}
