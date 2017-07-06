package org.netbeans.gradle.project.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class LazyValue<T> implements NbSupplier<T> {
    private final NbSupplier<? extends T> valueFactory;
    private final AtomicReference<T> valueRef;

    public LazyValue(NbSupplier<? extends T> valueFactory) {
        this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
        this.valueRef = new AtomicReference<>(null);
    }

    @Override
    public T get() {
        T result = valueRef.get();
        if (result == null) {
            result = valueFactory.get();
            if (!valueRef.compareAndSet(null, result)) {
                result = valueRef.get();
            }
        }
        return result;
    }
}
