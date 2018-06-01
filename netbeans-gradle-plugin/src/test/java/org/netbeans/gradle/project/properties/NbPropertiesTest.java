package org.netbeans.gradle.project.properties;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
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
    private static void runGC(WeakReference<?> testRef) {
        long startTime = System.nanoTime();
        int tryCount = 0;
        while (testRef.get() != null) {
            System.gc();
            System.gc();
            Runtime.getRuntime().runFinalization();
            tryCount++;

            if (tryCount >= 3 && (System.nanoTime() - startTime) > TimeUnit.MILLISECONDS.toNanos(20000)) {
                throw new AssertionError("Timeout while waiting for the GC.");
            }
        }
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
    }

    private static WeakReference<?> addWeakListener(
            PropertySource<?> property,
            final AtomicInteger listenerCallCount,
            AtomicReference<ListenerRef> resultRef) {
        // We do this in a separate method to decrease the chance that
        // some hidden local variable will keep a hard reference to the listener
        // or the ListenerRef.

        PropertySource<?> weakProperty = NbProperties.weakListenerProperty(property);
        Runnable listener = new Runnable() {
            @Override
            public void run() {
                listenerCallCount.incrementAndGet();
            }
        };

        WeakReference<?> testRef = new WeakReference<>(listener);

        resultRef.set(weakProperty.addChangeListener(listener));

        return testRef;
    }

    @Test
    public void testWeakListener() {
        AtomicReference<ListenerRef> listenerRef = new AtomicReference<>(null);
        final AtomicInteger listenerCallCount = new AtomicInteger(0);

        TestProperty<Integer> property = new TestProperty<>(0);
        WeakReference<?> testRef = addWeakListener(property, listenerCallCount, listenerRef);

        runGC(new WeakReference<>(new Object()));

        property.setValue(1);
        assertEquals("expected call count", 1, listenerCallCount.get());
        assertEquals("listener count", 1, property.getListenerCount());

        listenerRef.set(null);
        runGC(testRef);

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
