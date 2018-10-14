package org.netbeans.gradle.project.properties;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.junit.Ignore;
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
            AtomicInteger listenerCallCount,
            AtomicReference<ListenerRef> resultRef) {
        // We do this in a separate method to decrease the chance that
        // some hidden local variable will keep a hard reference to the listener
        // or the ListenerRef.

        PropertySource<?> weakProperty = NbProperties.weakListenerProperty(property);
        Runnable listener = listenerCallCount::incrementAndGet;

        WeakReference<?> testRef = new WeakReference<>(listener);
        resultRef.set(weakProperty.addChangeListener(listener));
        return testRef;
    }

    @Test
    @Ignore
    public void testWeakListener() throws InterruptedException {
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

        private final Lock listenersLock;
        private final Condition decreasedListenersCond;
        private final RefList<Runnable> listeners;

        public TestProperty(ValueType value) {
            this.value = value;
            this.listenersLock = new ReentrantLock();
            this.decreasedListenersCond = this.listenersLock.newCondition();
            this.listeners = new RefLinkedList<>();
        }

        public boolean tryWaitForNoListeners(long ms) throws InterruptedException {
            long nanos = TimeUnit.MILLISECONDS.toNanos(ms);
            listenersLock.lock();
            try {
                while (!listeners.isEmpty()) {
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = decreasedListenersCond.awaitNanos(nanos);
                }
            } finally {
                listenersLock.unlock();
            }
            return true;
        }

        public int getListenerCount() {
            listenersLock.lock();
            try {
                return listeners.size();
            } finally {
                listenersLock.unlock();
            }
        }

        public List<Runnable> getListeners() {
            listenersLock.lock();
            try {
                return new ArrayList<>(listeners);
            } finally {
                listenersLock.unlock();
            }
        }

        @Override
        public void setValue(ValueType value) {
            this.value = value;
            getListeners().forEach(Runnable::run);
        }

        @Override
        public ValueType getValue() {
            return value;
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            RefList.ElementRef<?> listRef;
            listenersLock.lock();
            try {
                listRef = listeners.addLastGetReference(listener);
            } finally {
                listenersLock.unlock();
            }

            return () -> {
                listenersLock.lock();
                try {
                    listRef.remove();
                    decreasedListenersCond.signalAll();
                } finally {
                    listenersLock.unlock();
                }
            };
        }
    }
}
