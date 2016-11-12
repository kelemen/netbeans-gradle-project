package org.netbeans.gradle.model.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class SharedTypesSerializationCache implements SerializationCache {
    private final Class<?>[] shareableTypes;
    private final Lock cacheLock;
    private final Map<Object, Object> cache;

    public SharedTypesSerializationCache(Class<?>... shareableTypes) {
        this.shareableTypes = shareableTypes;
        this.cacheLock = new ReentrantLock();
        this.cache = new HashMap<Object, Object>(256);

        for (Class<?> type: this.shareableTypes) {
            if (type == null) throw new NullPointerException("Shareable types must be non-null");
        }
    }

    public Object getCached(Object src) {
        for (Class<?> type: shareableTypes) {
            if (type.isInstance(src)) {
                cacheLock.lock();
                try {
                    Object result = cache.get(src);
                    if (result == null) {
                        result = src;
                        cache.put(result, result);
                    }
                    return result;
                } finally {
                    cacheLock.unlock();
                }
            }
        }
        return src;
    }
}
