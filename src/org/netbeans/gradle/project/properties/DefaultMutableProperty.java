package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

public final class DefaultMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private final boolean allowNulls;
    private ValueType value;
    private final ChangeSupport changes;

    public DefaultMutableProperty(ValueType value, boolean allowNulls) {
        if (!allowNulls) {
            if (value == null) throw new NullPointerException("value");
        }
        this.allowNulls = allowNulls;
        this.value = value;
        this.changes = new ChangeSupport(this);
    }

    @Override
    public void setValue(ValueType value) {
        if (!allowNulls) {
            if (value == null) throw new NullPointerException("value");
        }

        if (value == this.value) {
            return;
        }

        this.value = value;
        changes.fireChange();
    }

    @Override
    public ValueType getValue() {
        return value;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }
}
