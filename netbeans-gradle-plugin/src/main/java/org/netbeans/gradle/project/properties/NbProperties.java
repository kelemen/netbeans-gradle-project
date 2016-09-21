package org.netbeans.gradle.project.properties;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.util.NbBiFunction;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.gradle.project.view.AnnotationChildNodes;

public final class NbProperties {
    public static <Value> PropertySource<Value> weakListenerProperty(PropertySource<? extends Value> src) {
        return new WeakListenerProperty<>(src);
    }

    public static SimpleListenerRegistry<Runnable> asChangeListenerRegistry(
            final PropertySource<?> property) {
        ExceptionHelper.checkNotNullArgument(property, "property");

        return new SimpleListenerRegistry<Runnable>() {
            @Override
            public ListenerRef registerListener(Runnable listener) {
                return property.addChangeListener(listener);
            }
        };
    }

    public static <Value> PropertySource<Value> atomicValueView(
            final AtomicReference<? extends Value> valueRef,
            final SimpleListenerRegistry<Runnable> changeListeners) {
        ExceptionHelper.checkNotNullArgument(valueRef, "valueRef");
        ExceptionHelper.checkNotNullArgument(changeListeners, "changeListeners");

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

        return PropertyFactory.convert(wrapped, new ValueConverter<Integer, Boolean>() {
            @Override
            public Boolean convert(Integer input) {
                if (input == null) return null;
                return input <= maxValue && input >= minValue;
            }
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

    public static <RootValue, SubValue> PropertySource<SubValue> propertyOfProperty(
            PropertySource<? extends RootValue> rootSrc,
            NbFunction<? super RootValue, ? extends PropertySource<SubValue>> subPropertyGetter) {
        return new PropertyOfProperty<>(rootSrc, subPropertyGetter);
    }

    public static PropertySource<Boolean> isNotNull(PropertySource<?> src) {
        return PropertyFactory.convert(src, new ValueConverter<Object, Boolean>() {
            @Override
            public Boolean convert(Object input) {
                return input != null;
            }
        });
    }

    public static <T, U, R> PropertySource<R> combine(
            PropertySource<? extends T> src1,
            PropertySource<? extends U> src2,
            NbBiFunction<? super T, ? super U, ? extends R> valueCombiner) {
        return new CombinedProperties<>(src1, src2, valueCombiner);
    }

    public static <Value> PropertySource<Value> listSelection(final JList<? extends Value> list) {
        ExceptionHelper.checkNotNullArgument(list, "list");

        return new PropertySource<Value>() {
            @Override
            public Value getValue() {
                return list.getSelectedValue();
            }

            @Override
            public ListenerRef addChangeListener(final Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                final ListSelectionListener swingListener = new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        listener.run();
                    }
                };

                list.addListSelectionListener(swingListener);
                return NbListenerRefs.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        list.removeListSelectionListener(swingListener);
                    }
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
        ExceptionHelper.checkNotNullArgument(property, "property");
        final ChangeEvent event = new ChangeEvent(src);

        return SwingProperties.toSwingSource(property, new EventDispatcher<ChangeListener, Void>() {
            @Override
            public void onEvent(ChangeListener eventListener, Void arg) {
                eventListener.stateChanged(event);
            }
        });
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
        public boolean isRegistered() {
            return wrappedRef.isRegistered();
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

    private static class WeakListenerProperty<Value> implements PropertySource<Value> {
        private final PropertySource<? extends Value> src;

        public WeakListenerProperty(PropertySource<? extends Value> src) {
            ExceptionHelper.checkNotNullArgument(src, "src");

            this.src = src;
        }

        @Override
        public Value getValue() {
            return src.getValue();
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            final WeakReference<Runnable> listenerRef = new WeakReference<>(listener);
            ListenerRef result = src.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    Runnable listener = listenerRef.get();
                    if (listener != null) {
                        listener.run();
                    }
                }
            });

            return new ReferenceHolderListenerRef(listener, result);
        }
    }

    private static final class CombinedProperties<R> implements PropertySource<R> {
        private final PropertySource<?> src1;
        private final PropertySource<?> src2;
        private final CombinedValues<?, ?, ? extends R> valueRef;

        public <T, U> CombinedProperties(
                PropertySource<? extends T> src1,
                PropertySource<? extends U> src2,
                NbBiFunction<? super T, ? super U, ? extends R> valueCombiner) {
            ExceptionHelper.checkNotNullArgument(src1, "src1");
            ExceptionHelper.checkNotNullArgument(src2, "src2");
            ExceptionHelper.checkNotNullArgument(valueCombiner, "valueCombiner");

            this.src1 = src1;
            this.src2 = src2;
            this.valueRef = new CombinedValues<>(src1, src2, valueCombiner);
        }

        @Override
        public R getValue() {
            return valueRef.getValue();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return ListenerRegistries.combineListenerRefs(
                    src1.addChangeListener(listener),
                    src2.addChangeListener(listener));
        }
    }

    private static final class CombinedValues<T, U, R> {
        private final PropertySource<? extends T> src1;
        private final PropertySource<? extends U> src2;
        private final NbBiFunction<? super T, ? super U, ? extends R> valueCombiner;

        public CombinedValues(
                PropertySource<? extends T> src1,
                PropertySource<? extends U> src2,
                NbBiFunction<? super T, ? super U, ? extends R> valueCombiner) {
            this.src1 = src1;
            this.src2 = src2;
            this.valueCombiner = valueCombiner;
        }

        public R getValue() {
            T value1 = src1.getValue();
            U value2 = src2.getValue();
            return valueCombiner.apply(value1, value2);
        }
    }
}
