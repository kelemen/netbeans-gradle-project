package org.netbeans.gradle.project.properties;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.openide.util.ChangeSupport;
import org.openide.util.Utilities;

public final class DefaultMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private static final Logger LOGGER = Logger.getLogger(DefaultMutableProperty.class.getName());

    private final boolean allowNulls;
    private volatile PropertySource<? extends ValueType> valueSource;
    private final Lock changesLock;
    private final ChangeSupport changes;
    private final ChangeListener changeForwarder;

    public DefaultMutableProperty(ValueType value, boolean defaultValue, boolean allowNulls) {
        this(asSource(value, defaultValue, allowNulls), allowNulls);
    }

    public DefaultMutableProperty(
            final PropertySource<? extends ValueType> initialValue,
            boolean allowNulls) {
        ExceptionHelper.checkNotNullArgument(initialValue, "initialValue");

        this.allowNulls = allowNulls;
        this.valueSource = initialValue;
        this.changesLock = new ReentrantLock();
        this.changes = new ChangeSupport(this);
        this.changeForwarder = new ChangeListener() {
            private ValueType prevValue = initialValue.getValue();

            @Override
            public void stateChanged(ChangeEvent e) {
                ValueType value = prevValue;
                ValueType newValue = getValue();
                prevValue = newValue;

                if (!Utilities.compareObjects(value, newValue)) {
                    changes.fireChange();
                }
            }
        };
    }

    private static <ValueType> PropertySource<? extends ValueType> asSource(
            ValueType value, boolean defaultValue, boolean allowNulls) {
        return new ConstPropertySource<>(
                allowNulls ? value : checkNull(value),
                defaultValue);
    }

    private static <T> T checkNull(T value) {
        ExceptionHelper.checkNotNullArgument(value, "value");
        return value;
    }

    @Override
    public void setValueFromSource(PropertySource<? extends ValueType> source) {
        ExceptionHelper.checkNotNullArgument(source, "source");

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
            ExceptionHelper.checkNotNullArgument(value, "value");
        }

        setValueFromSource(new ConstPropertySource<>(value, false));
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
