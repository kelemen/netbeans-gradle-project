package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;

public final class ConstPropertySource<ValueType>
implements
        OldPropertySource<ValueType> {

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
    public ListenerRef addChangeListener(Runnable listener) {
        return UnregisteredListenerRef.INSTANCE;
    }
}
