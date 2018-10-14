package org.netbeans.gradle.project.properties;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.event.SimpleListenerRegistry;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.jtrim2.property.swing.SwingPropertySource;
import org.netbeans.gradle.project.util.EventUtils;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;

public final class NbProperties {
    public static <Value> PropertySource<Value> weakListenerProperty(PropertySource<? extends Value> src) {
        return new WeakListenerProperty<>(src);
    }

    public static SimpleListenerRegistry<Runnable> weakListenerRegistry(SimpleListenerRegistry<Runnable> wrapped) {
        return new WeakChangeListenerRegistry(wrapped);
    }

    public static <Value> PropertySource<Value> atomicValueView(
            final AtomicReference<? extends Value> valueRef,
            final SimpleListenerRegistry<Runnable> changeListeners) {
        Objects.requireNonNull(valueRef, "valueRef");
        Objects.requireNonNull(changeListeners, "changeListeners");

        return new PropertySource<Value>() {
            @Override
            public Value getValue() {
                return valueRef.get();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return changeListeners.registerListener(listener);
            }
        };
    }

    public static PropertySource<Boolean> between(
            final PropertySource<Integer> wrapped,
            final int minValue,
            final int maxValue) {

        return PropertyFactory.convert(wrapped, (Integer input) -> {
            if (input == null) return null;
            return input <= maxValue && input >= minValue;
        });
    }

    public static PropertySource<Boolean> greaterThanOrEqual(
            final PropertySource<Integer> wrapped,
            final int value) {
        return between(wrapped, value, Integer.MAX_VALUE);
    }

    public static PropertySource<Boolean> lessThanOrEqual(
            final PropertySource<Integer> wrapped,
            final int value) {
        return between(wrapped, Integer.MIN_VALUE, value);
    }

    public static PropertySource<Boolean> isNotNull(PropertySource<?> src) {
        return PropertyFactory.convert(src, input -> input != null);
    }

    public static <Value> PropertySource<Value> listSelection(final JList<? extends Value> list) {
        Objects.requireNonNull(list, "list");

        return new PropertySource<Value>() {
            @Override
            public Value getValue() {
                return list.getSelectedValue();
            }

            @Override
            public ListenerRef addChangeListener(final Runnable listener) {
                Objects.requireNonNull(listener, "listener");

                ListSelectionListener swingListener = e -> listener.run();

                list.addListSelectionListener(swingListener);
                return EventUtils.asSafeListenerRef(() -> {
                    list.removeListSelectionListener(swingListener);
                });
            }
        };
    }

    public static <Value> SwingPropertySource<Value, ChangeListener> toOldProperty(
            PropertySource<? extends Value> property) {
        return toOldProperty(property, property);
    }

    public static <Value> SwingPropertySource<Value, ChangeListener> toOldProperty(
            PropertySource<? extends Value> property,
            Object src) {
        Objects.requireNonNull(property, "property");
        final ChangeEvent event = new ChangeEvent(src);

        return SwingProperties.toSwingSource(property, (ChangeListener eventListener, Void arg) -> {
            eventListener.stateChanged(event);
        });
    }

    public static <T> PropertySource<T> cacheFirstNonNull(final PropertySource<? extends T> src) {
        return new CacheFirstNonNullProperty<>(src);
    }

    public static <T> PropertySource<T> lookupProperty(Lookup lookup, Class<? extends T> type) {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(type, "type");

        Lookup.Result<? extends T> lookupResult = lookup.lookupResult(type);
        LookupResultProperty<T> oldProperty = new LookupResultProperty<>(lookupResult);
        return SwingProperties.fromSwingSource(oldProperty, (Runnable listener) -> {
            return ev -> listener.run();
        });
    }

    private static final class CacheFirstNonNullProperty<T> implements PropertySource<T> {
        private final PropertySource<? extends T> src;
        private final AtomicReference<T> cache;

        public CacheFirstNonNullProperty(PropertySource<? extends T> src) {
            this.src = Objects.requireNonNull(src, "src");
            this.cache = new AtomicReference<>(null);
        }

        @Override
        public T getValue() {
            T result = cache.get();
            if (result == null) {
                result = src.getValue();
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            if (cache.get() != null) {
                return ListenerRefs.unregistered();
            }

            return src.addChangeListener(new Runnable() {
                private volatile boolean stopNotifying = false;

                @Override
                public void run() {
                    if (!stopNotifying) {
                        stopNotifying = cache.get() != null;
                        listener.run();
                    }
                }
            });
        }
    }

    private static final class LookupResultProperty<T> implements SwingPropertySource<T, LookupListener> {
        private final Lookup.Result<? extends T> lookupResult;

        public LookupResultProperty(Lookup.Result<? extends T> lookupResult) {
            this.lookupResult = Objects.requireNonNull(lookupResult, "lookupResult");
        }

        @Override
        public T getValue() {
            Iterator<? extends T> resultItr = lookupResult.allInstances().iterator();
            return resultItr.hasNext() ? resultItr.next() : null;
        }

        @Override
        public void addChangeListener(LookupListener listener) {
            lookupResult.addLookupListener(listener);
        }

        @Override
        public void removeChangeListener(LookupListener listener) {
            lookupResult.removeLookupListener(listener);
        }
    }

    private static class ReferenceHolderListenerRef implements ListenerRef {
        private final Runnable listener;
        private final ListenerRef wrappedRef;

        public ReferenceHolderListenerRef(Runnable listener, ListenerRef wrappedRef) {
            // We won't do anything useful with listener, just storing a reference
            // to it to prevent it from being garbage collected.
            this.listener = listener;
            this.wrappedRef = wrappedRef;
        }

        public Runnable getListener() {
            return listener;
        }

        @Override
        public void unregister() {
            wrappedRef.unregister();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            wrappedRef.unregister();
            super.finalize();
        }
    }

    private static class WeakChangeListenerRegistry implements SimpleListenerRegistry<Runnable> {
        private final SimpleListenerRegistry<Runnable> wrapped;

        public WeakChangeListenerRegistry(SimpleListenerRegistry<Runnable> wrapped) {
            this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
        }

        @Override
        public ListenerRef registerListener(Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            final WeakReference<Runnable> listenerRef = new WeakReference<>(listener);
            ListenerRef result = wrapped.registerListener(() -> {
                Runnable currentListener = listenerRef.get();
                if (currentListener != null) {
                    currentListener.run();
                }
            });

            return new ReferenceHolderListenerRef(listener, result);
        }
    }

    private static class WeakListenerProperty<Value> implements PropertySource<Value> {
        private final PropertySource<? extends Value> src;
        private final SimpleListenerRegistry<Runnable> changeRegistry;

        public WeakListenerProperty(PropertySource<? extends Value> src) {
            this.src = Objects.requireNonNull(src, "src");
            this.changeRegistry = new WeakChangeListenerRegistry(src::addChangeListener);
        }

        @Override
        public Value getValue() {
            return src.getValue();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return changeRegistry.registerListener(listener);
        }
    }
}
