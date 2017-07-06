package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;

public final class AtomicIntProperty implements MutableProperty<Integer> {
    private final AtomicInteger value;
    private final ChangeListenerManager listeners;

    public AtomicIntProperty(TaskExecutor eventExecutor) {
        this.value = new AtomicInteger(0);
        this.listeners = new GenericChangeListenerManager(eventExecutor);
    }

    private void fireChangeEvent() {
        listeners.fireEventually();
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
