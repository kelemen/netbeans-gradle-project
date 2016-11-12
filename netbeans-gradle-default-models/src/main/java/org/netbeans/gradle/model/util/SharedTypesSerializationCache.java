package org.netbeans.gradle.model.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class SharedTypesSerializationCache implements SerializationCache {
    private final Lock cacheLock;
    private final Map<Object, Object> cache;

    public SharedTypesSerializationCache() {
        this.cacheLock = new ReentrantLock();
        this.cache = new HashMap<Object, Object>(256);
    }

    public Object getCached(Object src) {
        if (src instanceof File) {
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

        return src;
    }
}
