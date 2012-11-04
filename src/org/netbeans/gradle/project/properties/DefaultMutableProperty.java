package org.netbeans.gradle.project.properties;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

public final class DefaultMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private static final Logger LOGGER = Logger.getLogger(DefaultMutableProperty.class.getName());

    private final boolean allowNulls;
    private PropertySource<? extends ValueType> valueSource;
    private final Lock changesLock;
    private final ChangeSupport changes;
    private final ChangeListener changeForwarder;

    public DefaultMutableProperty(ValueType value, boolean defaultValue, boolean allowNulls) {
        if (!allowNulls) {
            if (value == null) throw new NullPointerException("value");
        }
        this.allowNulls = allowNulls;
        this.valueSource = new ConstPropertySource<ValueType>(value, defaultValue);
        this.changesLock = new ReentrantLock();
        this.changes = new ChangeSupport(this);
        this.changeForwarder = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                changes.fireChange();
            }
        };
    }

    @Override
    public void setValueFromSource(PropertySource<? extends ValueType> source) {
        if (source == null) throw new NullPointerException("source");

        changesLock.lock();
        try {
            boolean hasListeners = changes.hasListeners();
            if (hasListeners) {
                this.valueSource.removeChangeListener(changeForwarder);
            }

            this.valueSource = source;

            if (hasListeners) {
                source.addChangeListener(changeForwarder);
            }
        } finally {
            changesLock.unlock();
        }
        changes.fireChange();
    }

    @Override
    public void setValue(ValueType value) {
        if (!allowNulls) {
            if (value == null) throw new NullPointerException("value");
        }

        setValueFromSource(new ConstPropertySource<ValueType>(value, false));
    }

    @Override
    public ValueType getValue() {
        ValueType value = valueSource.getValue();
        if (value == null && !allowNulls) {
            String message = "The value of the property is null but null values are not permitted.";
            LOGGER.log(Level.SEVERE, message, new Exception(message));
        }
        return value;
    }

    @Override
    public boolean isDefault() {
        return valueSource.isDefault();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changesLock.lock();
        try {
            boolean addedNow = !changes.hasListeners();
            changes.addChangeListener(listener);
            if (addedNow) {
                valueSource.addChangeListener(changeForwarder);
            }
        } finally {
            changesLock.unlock();
        }
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changesLock.lock();
        try {
            if (!changes.hasListeners()) {
                return;
            }

            changes.removeChangeListener(listener);
            if (!changes.hasListeners()) {
                valueSource.removeChangeListener(changeForwarder);
            }
        } finally {
            changesLock.unlock();
        }
    }
}
