package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
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
    }

    private static void addWeakListener(
            PropertySource<?> property,
            final AtomicInteger listenerCallCount,
            AtomicReference<ListenerRef> resultRef) {
        // We do this in a separate method to decrease the chance that
        // some hidden local variable will keep a hard reference to the listener
        // or the ListenerRef.

        PropertySource<?> weakProperty = NbProperties.weakListenerProperty(property);
        resultRef.set(weakProperty.addChangeListener(new Runnable() {
            @Override
            public void run() {
                listenerCallCount.incrementAndGet();
            }
        }));
    }

    @Test
    public void testWeakListener() {
        AtomicReference<ListenerRef> listenerRef = new AtomicReference<>(null);
        final AtomicInteger listenerCallCount = new AtomicInteger(0);

        MutableProperty<Integer> property = PropertyFactory.memProperty(0);
        addWeakListener(property, listenerCallCount, listenerRef);

        runGC();

        property.setValue(1);
        assertEquals("expected call count", 1, listenerCallCount.get());

        listenerRef.set(null);
        runGC();

        property.setValue(2);
        assertEquals("expected call count", 1, listenerCallCount.get());
    }
}
