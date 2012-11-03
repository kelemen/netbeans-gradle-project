package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public final class ConstPropertySource<ValueType>
implements
        PropertySource<ValueType> {

    private final ValueType value;
    private final boolean defaultValue;

    public ConstPropertySource(ValueType value, boolean defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    @Override
    public ValueType getValue() {
        return value;
    }

    @Override
    public boolean isDefault() {
        return defaultValue;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
    }
}
