package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;

public interface PropertySource<ValueType> extends org.jtrim.property.PropertySource<ValueType> {
    @Override
    public ValueType getValue();

    @Override
    public ListenerRef addChangeListener(Runnable listener);

    public boolean isDefault();
}
