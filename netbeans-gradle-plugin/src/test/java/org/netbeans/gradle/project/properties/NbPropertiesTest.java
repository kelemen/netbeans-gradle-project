package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.junit.Test;

import static org.junit.Assert.*;

public class NbPropertiesTest {
    private static void runGC() {
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
    }

    private static void addWeakListener(
            PropertySource<?> property,
            AtomicInteger listenerCallCount,
            AtomicReference<ListenerRef> resultRef) {
        // We do this in a separate method to decrease the chance that
        // some hidden local variable will keep a hard reference to the listener
        // or the ListenerRef.

        PropertySource<?> weakProperty = NbProperties.weakListenerProperty(property);
        resultRef.set(weakProperty.addChangeListener(listenerCallCount::incrementAndGet));
    }

    @Test
    public void testWeakListener() {
        AtomicReference<ListenerRef> listenerRef = new AtomicReference<>(null);
        final AtomicInteger listenerCallCount = new AtomicInteger(0);

        TestProperty<Integer> property = new TestProperty<>(0);
        addWeakListener(property, listenerCallCount, listenerRef);

        runGC();

        property.setValue(1);
        assertEquals("expected call count", 1, listenerCallCount.get());
        assertEquals("listener count", 1, property.getListenerCount());

        listenerRef.set(null);
        runGC();

        property.setValue(2);
        assertEquals("expected call count", 1, listenerCallCount.get());
        assertEquals("listener count", 0, property.getListenerCount());
    }

    private static final class TestProperty<ValueType> implements MutableProperty<ValueType> {
        private volatile ValueType value;
        private final ListenerManager<Runnable> listeners;

        public TestProperty(ValueType value) {
            this.value = value;
            this.listeners = new CopyOnTriggerListenerManager<>();
        }

        public int getListenerCount() {
            return listeners.getListenerCount();
        }

        @Override
        public void setValue(ValueType value) {
            this.value = value;
            EventListeners.dispatchRunnable(listeners);
        }

        @Override
        public ValueType getValue() {
            return value;
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return listeners.registerListener(listener);
        }
    }
}
