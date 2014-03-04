package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;

public interface OldMutableProperty<ValueType> {
    // Getting the value of a property is thread-safe but setting it must be
    // done on the EDT.
    // The only exception is that if the property has just been created and it
    // is also known that registered listeners can safely be accessed from
    // the thread setting the value.
    public void setValueFromSource(OldPropertySource<? extends ValueType> source);
    public void setValue(ValueType value);
    public ValueType getValue();

    public boolean isDefault();

    public ListenerRef addChangeListener(Runnable listener);
}
