package org.netbeans.gradle.model.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SharedTypesSerializationCache implements SerializationCache {
    private final Class<?>[] shareableTypes;
    private final ConcurrentMap<Object, Object> cache;

    public SharedTypesSerializationCache(Class<?>... shareableTypes) {
        this.shareableTypes = shareableTypes;
        this.cache = new ConcurrentHashMap<Object, Object>(256);

        for (Class<?> type: this.shareableTypes) {
            if (type == null) throw new NullPointerException("Shareable types must be non-null");
        }
    }

    @Override
    public Object getCached(Object src) {
        for (Class<?> type: shareableTypes) {
            if (type.isInstance(src)) {
                Object prevValue = cache.putIfAbsent(src, src);
                return prevValue != null ? prevValue : src;
            }
        }
        return src;
    }
}
