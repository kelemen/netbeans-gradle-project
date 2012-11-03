package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public final class ConstPropertySource<ValueType>
implements
        PropertySource<ValueType> {

    private final ValueType value;

    public ConstPropertySource(ValueType value) {
        this.value = value;
    }

    @Override
    public ValueType getValue() {
        return value;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
    }
}
