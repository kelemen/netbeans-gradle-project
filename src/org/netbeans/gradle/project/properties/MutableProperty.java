package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public interface MutableProperty<ValueType> {
    public void setValue(ValueType value);
    public ValueType getValue();

    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
