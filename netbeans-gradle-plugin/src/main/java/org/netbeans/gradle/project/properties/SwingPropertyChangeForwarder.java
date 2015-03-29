package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.InitLaterListenerRef;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class SwingPropertyChangeForwarder {
    public static final class Builder {
        private final UpdateTaskExecutor eventExecutor;
        private final List<NamedProperty> properties;

        public Builder() {
            this((UpdateTaskExecutor)null);
        }

        public Builder(TaskExecutor eventExecutor) {
            this(new GenericUpdateTaskExecutor(eventExecutor));
        }

        public Builder(UpdateTaskExecutor eventExecutor) {
            this.eventExecutor = eventExecutor;
            this.properties = new LinkedList<>();
        }

        public void addProperty(String name, PropertySource<?> property, Object source) {
            properties.add(new NamedProperty(name, property, source));
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
    private final UpdateTaskExecutor eventExecutor;

    private SwingPropertyChangeForwarder(Builder builder) {
        this.mainLock = new ReentrantLock();
        this.listeners = new HashMap<>();
        this.properties = CollectionsEx.readOnlyCopy(builder.properties);
        this.eventExecutor = builder.eventExecutor;
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        // FIXME: It is possible that the passed listener "equals" to an
        //   already existing one but is not the same. In this case, it would
        //   be better to call both instance separately. Though the client
        //   code should not rely on such a behaviour.

        InitLaterListenerRef combinedRef = new InitLaterListenerRef();

        mainLock.lock();
        try {
            RegistrationRef currentRef = listeners.get(listener);
            if (currentRef != null) {
                currentRef.incRegCount();
                return;
            }

            listeners.put(listener, new RegistrationRef(combinedRef));
        } finally {
            mainLock.unlock();
        }

        List<ListenerRef> refs = new ArrayList<>(properties.size());
        for (NamedProperty namedProperty: properties) {
            if (name == null || Objects.equals(name, namedProperty.name)) {
                namedProperty.property.addChangeListener(namedProperty.forwarderTask(eventExecutor, listener));
            }
        }

        combinedRef.init(ListenerRegistries.combineListenerRefs(refs));
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

        mainLock.lock();
        try {
            RegistrationRef regRef = listeners.get(listener);
            if (regRef != null) {
                if (regRef.decRegCount()) {
                    regRef.unregister();
                    listeners.remove(listener);
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private static final class RegistrationRef {
        private final ListenerRef ref;
        private int regCount;

        public RegistrationRef(ListenerRef ref) {
            this.ref = ref;
            this.regCount = 1;
        }

        public void incRegCount() {
            regCount++;
        }

        public boolean decRegCount() {
            regCount--;
            return regCount <= 0;
        }

        public void unregister() {
            ref.unregister();
        }
    }

    private static final class NamedProperty {
        public final String name;
        public final PropertySource<?> property;
        public final Object source;

        public NamedProperty(String name, PropertySource<?> property, Object source) {
            ExceptionHelper.checkNotNullArgument(name, "name");
            ExceptionHelper.checkNotNullArgument(property, "property");
            ExceptionHelper.checkNotNullArgument(source, "source");

            this.name = name;
            this.property = property;
            this.source = source;
        }

        private PropertyChangeEvent getChangeEvent() {
            return new PropertyChangeEvent(source, name, null, property.getValue());
        }

        public Runnable forwarderTask(final UpdateTaskExecutor eventExecutor, PropertyChangeListener listener) {
            final Runnable forwardNowTask = directForwarderTask(listener);
            if (eventExecutor == null) {
                return forwardNowTask;
            }

            return new Runnable() {
                @Override
                public void run() {
                    eventExecutor.execute(forwardNowTask);
                }
            };
        }

        public Runnable directForwarderTask(final PropertyChangeListener listener) {
            return new Runnable() {
                @Override
                public void run() {
                    listener.propertyChange(getChangeEvent());
                }
            };
        }
    }
}
