package org.netbeans.gradle.model.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

public final class ConstructableWeakRef<T> implements NbSupplier5<T> {
    private final NbSupplier5<? extends T> objFactory;
    private final AtomicReference<WeakReference<T>> defaultRefRef;

    public ConstructableWeakRef(NbSupplier5<? extends T> objFactory) {
        if (objFactory == null) throw new NullPointerException("objFactory");
        this.objFactory = objFactory;
        this.defaultRefRef = new AtomicReference<WeakReference<T>>();
    }

    @Override
    public T get() {
        while (true) {
            WeakReference<T> defaultRef = defaultRefRef.get();
            T defaultCache = defaultRef != null ? defaultRef.get() : null;

            if (defaultCache == null) {
                defaultCache = objFactory.get();
                if (!defaultRefRef.compareAndSet(defaultRef, new WeakReference<T>(defaultCache))) {
                    continue;
                }
            }
            return defaultCache;
        }
    }
}
