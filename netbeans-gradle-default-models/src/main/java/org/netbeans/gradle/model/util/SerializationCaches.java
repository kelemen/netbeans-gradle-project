package org.netbeans.gradle.model.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

public final class SerializationCaches {
    private static final AtomicReference<WeakReference<SerializationCache>> DEFAULT_REF_REF
            = new AtomicReference<WeakReference<SerializationCache>>();

    public static SerializationCache getDefault() {
        while (true) {
            WeakReference<SerializationCache> defaultRef = DEFAULT_REF_REF.get();
            SerializationCache defaultCache = defaultRef != null ? defaultRef.get() : null;

            if (defaultCache == null) {
                defaultCache = createDefault();
                if (!DEFAULT_REF_REF.compareAndSet(defaultRef, new WeakReference<SerializationCache>(defaultCache))) {
                    continue;
                }
            }
            return defaultCache;
        }
    }

    private static SerializationCache createDefault() {
        return new SharedTypesSerializationCache(File.class);
    }

    private SerializationCaches() {
        throw new AssertionError();
    }
}
