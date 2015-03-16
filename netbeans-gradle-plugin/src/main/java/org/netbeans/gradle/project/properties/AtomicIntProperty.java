package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;

public final class AtomicIntProperty implements MutableProperty<Integer> {
    private final AtomicInteger value;
    private final ListenerManager<Runnable> listeners;
    private final UpdateTaskExecutor eventExecutor;
    private final Runnable listenerForwarder;

    public AtomicIntProperty() {
        this.value = new AtomicInteger(0);
        this.listeners = new CopyOnTriggerListenerManager<>();
        this.eventExecutor = new SwingUpdateTaskExecutor(false);
        this.listenerForwarder = new Runnable() {
            @Override
            public void run() {
                EventListeners.dispatchRunnable(listeners);
            }
        };
    }

    private void fireChangeEvent() {
        eventExecutor.execute(listenerForwarder);
    }

    public int getAndIncrement() {
        int result = value.getAndIncrement();
        fireChangeEvent();
        return result;
    }

    public int getAndDecrement() {
        int result = value.getAndDecrement();
        fireChangeEvent();
        return result;
    }

    public int getIntValue() {
        return value.get();
    }

    public void setIntValue(int value) {
        this.value.set(value);
        fireChangeEvent();
    }

    @Override
    public void setValue(Integer value) {
        setIntValue(value);
    }

    @Override
    public Integer getValue() {
        return getIntValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return listeners.registerListener(listener);
    }
}
