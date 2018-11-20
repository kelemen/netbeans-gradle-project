package org.netbeans.gradle.model.util;

public interface SerializationCache {
    public static final SerializationCache NO_CACHE = new SerializationCache() {
        @Override
        public Object getCached(Object src) {
            return src;
        }
    };

    public Object getCached(Object src);
}
