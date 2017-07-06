package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.InitLaterListenerRef;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.PropertySource;

public final class SwingPropertyChangeForwarder {
    public static final class Builder {
        private final TaskExecutor eventExecutorSrc;
        private final UpdateTaskExecutor eventExecutor;
        private final List<NamedProperty> properties;

        public Builder() {
            this(null, null);
        }

        public Builder(TaskExecutor eventExecutor) {
            this(eventExecutor, new GenericUpdateTaskExecutor(eventExecutor));
        }

        private Builder(TaskExecutor eventExecutorSrc, UpdateTaskExecutor eventExecutor) {
            this.eventExecutorSrc = eventExecutorSrc;
            this.eventExecutor = eventExecutor;
            this.properties = new ArrayList<>();
        }

        public void addPropertyNoValue(String name, PropertySource<?> property, Object source) {
            properties.add(new NamedProperty(name, property, source, false));
        }

        public void addPropertyNoValue(String name, PropertySource<?> property) {
            addPropertyNoValue(name, property, property);
        }

        public void addProperty(String name, PropertySource<?> property, Object source) {
            properties.add(new NamedProperty(name, property, source, true));
        }

        public void addProperty(String name, PropertySource<?> property) {
            addProperty(name, property, property);
        }

        public SwingPropertyChangeForwarder create() {
            return new SwingPropertyChangeForwarder(this);
        }
    }

    private final Lock mainLock;
    private final Map<PropertyChangeListener, RegistrationRef> listeners;
    private final List<NamedProperty> properties;

    private final TaskExecutor eventExecutorSrc;
    private final UpdateTaskExecutor eventExecutor;

    private SwingPropertyChangeForwarder(Builder builder) {
        this.mainLock = new ReentrantLock();
        this.listeners = new HashMap<>();
        this.properties = CollectionsEx.readOnlyCopy(builder.properties);
        this.eventExecutorSrc = builder.eventExecutorSrc;
        this.eventExecutor = builder.eventExecutor;
    }

    private void fireListeners(PropertyChangeEvent changeEvent, List<PropertyChangeListener> toCall) {
        for (PropertyChangeListener listener: toCall) {
            listener.propertyChange(changeEvent);
        }
    }

    public void firePropertyChange(final PropertyChangeEvent changeEvent) {
        Objects.requireNonNull(changeEvent, "changeEvent");

        String name = changeEvent.getPropertyName();

        boolean addedAll = true;

        final List<PropertyChangeListener> toCall;
        mainLock.lock();
        try {
            toCall = new ArrayList<>(listeners.size());

            for (RegistrationRef ref: listeners.values()) {
                boolean currentAddedAll = ref.addListeners(name, toCall);
                addedAll = addedAll && currentAddedAll;
            }
        } finally {
            mainLock.unlock();
        }

        if (eventExecutor == null) {
            fireListeners(changeEvent, toCall);
        }
        else {
            if (addedAll) {
                eventExecutor.execute(() -> {
                    fireListeners(changeEvent, toCall);
                });
            }
            else {
                eventExecutorSrc.execute(() -> fireListeners(changeEvent, toCall));
            }
        }
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        InitLaterListenerRef combinedRef = new InitLaterListenerRef();
        RegisteredListener registeredListener = new RegisteredListener(name, listener, combinedRef);

        mainLock.lock();
        try {
            RegistrationRef currentRef = listeners.get(listener);
            if (currentRef != null) {
                currentRef.incRegCount(registeredListener);
            }
            else {
                listeners.put(listener, new RegistrationRef(registeredListener));
            }
        } finally {
            mainLock.unlock();
        }

        List<ListenerRef> refs = new ArrayList<>(properties.size());
        for (NamedProperty namedProperty: properties) {
            if (name == null || Objects.equals(name, namedProperty.name)) {
                refs.add(namedProperty.property.addChangeListener(namedProperty.forwarderTask(eventExecutor, listener)));
            }
        }

        combinedRef.init(ListenerRefs.combineListenerRefs(refs));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy listenerProxy = (PropertyChangeListenerProxy)listener;
            addPropertyChangeListener(listenerProxy.getPropertyName(), listenerProxy.getListener());
        }
        else {
            addPropertyChangeListener(null, listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        ListenerRef toUnregister = null;
        mainLock.lock();
        try {
            RegistrationRef regRef = listeners.get(listener);
            if (regRef != null) {
                toUnregister = regRef.decRegCount(listener);
                if (regRef.isCompletelyUnregistered()) {
                    listeners.remove(listener);
                }
            }
        } finally {
            mainLock.unlock();
        }

        if (toUnregister != null) {
            toUnregister.unregister();
        }
    }

    // For testing purposes
    void checkListenerConsistency() {
        mainLock.lock();
        try {
            for (RegistrationRef ref: listeners.values()) {
                if (ref.listeners.isEmpty()) {
                    throw new AssertionError("There are no listeners and the listener map still contains the reference.");
                }

                if (ref.listeners.size() != ref.regCount) {
                    throw new AssertionError("Number of listeners is not equal to registration count.");
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private static final class RegistrationRef {
        private final List<RegisteredListener> listeners;
        private int regCount;

        public RegistrationRef(RegisteredListener listener) {
            this.regCount = 1;
            this.listeners = new LinkedList<>();
            this.listeners.add(listener);
        }

        public void incRegCount(RegisteredListener listener) {
            regCount++;

            listeners.add(listener);
        }

        public ListenerRef decRegCount(PropertyChangeListener listener) {
            regCount--;

            Iterator<RegisteredListener> listenersItr = listeners.iterator();
            while (listenersItr.hasNext()) {
                RegisteredListener currentListener = listenersItr.next();
                if (Objects.equals(currentListener.listener, listener)) {
                    listenersItr.remove();
                    return currentListener.listenerRef;
                }
            }

            return null;
        }

        public boolean isCompletelyUnregistered() {
            return regCount <= 0;
        }

        private boolean addListeners(String name, Collection<? super PropertyChangeListener> result) {
            boolean addedAll = true;
            for (RegisteredListener listener: listeners) {
                if (name == null || listener.name == null || Objects.equals(name, listener.name)) {
                    result.add(listener.listener);
                }
                else {
                    addedAll = false;
                }
            }
            return addedAll;
        }
    }

    private static final class RegisteredListener {
        public final String name;
        public final PropertyChangeListener listener;
        public final ListenerRef listenerRef;

        public RegisteredListener(String name, PropertyChangeListener listener, ListenerRef listenerRef) {
            this.name = name;
            this.listener = listener;
            this.listenerRef = listenerRef;
        }
    }

    private static final class NamedProperty {
        public final String name;
        public final PropertySource<?> property;
        public final Object source;
        private final boolean forwardValue;

        public NamedProperty(String name, PropertySource<?> property, Object source, boolean forwardValue) {
            this.name = Objects.requireNonNull(name, "name");
            this.property = Objects.requireNonNull(property, "property");
            this.source = Objects.requireNonNull(source, "source");
            this.forwardValue = forwardValue;
        }

        private PropertyChangeEvent getChangeEventWithValue() {
            return new PropertyChangeEvent(source, name, null, property.getValue());
        }

        public Runnable forwarderTask(final UpdateTaskExecutor eventExecutor, PropertyChangeListener listener) {
            final Runnable forwardNowTask = directForwarderTask(listener);
            if (eventExecutor == null) {
                return forwardNowTask;
            }

            return () -> eventExecutor.execute(forwardNowTask);
        }

        public Runnable directForwarderTask(final PropertyChangeListener listener) {
            if (forwardValue) {
                return () -> listener.propertyChange(getChangeEventWithValue());
            }
            else {
                PropertyChangeEvent event = new PropertyChangeEvent(source, name, null, null);
                return () -> listener.propertyChange(event);
            }
        }
    }
}
