package org.netbeans.gradle.project.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.Lookup;

public final class CachedLookupValue<ValueType> {
    private final Lookup.Provider provider;
    private final Class<? extends ValueType> type;
    private final AtomicReference<ValueType> valueRef;

    public CachedLookupValue(Lookup.Provider provider, Class<? extends ValueType> type) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.type = Objects.requireNonNull(type, "type");
        this.valueRef = new AtomicReference<>(null);
    }

    public ValueType get() {
        ValueType result = valueRef.get();
        if (result == null) {
            result = provider.getLookup().lookup(type);
            if (!valueRef.compareAndSet(null, result)) {
                result = valueRef.get();
            }
        }
        return result;
    }
}
