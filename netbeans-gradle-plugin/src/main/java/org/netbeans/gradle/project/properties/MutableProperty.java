package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public interface MutableProperty<ValueType> {
    // Getting the value of a property is thread-safe but setting it must be
    // done on the EDT.
    // The only exception is that if the property has just been created and it
    // is also known that registered listeners can safely be accessed from
    // the thread setting the value.
    public void setValueFromSource(PropertySource<? extends ValueType> source);
    public void setValue(ValueType value);
    public ValueType getValue();

    public boolean isDefault();

    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
