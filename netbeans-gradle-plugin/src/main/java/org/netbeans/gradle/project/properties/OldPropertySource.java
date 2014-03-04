package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;

public interface OldPropertySource<ValueType> extends PropertySource<ValueType> {
    @Override
    public ValueType getValue();

    @Override
    public ListenerRef addChangeListener(Runnable listener);

    public boolean isDefault();
}
