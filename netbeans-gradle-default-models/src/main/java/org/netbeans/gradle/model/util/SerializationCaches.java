package org.netbeans.gradle.model.util;

import java.io.File;

public final class SerializationCaches {
    private static final NbSupplier5<SerializationCache> DEFAULT_REF = new ConstructableWeakRef<SerializationCache>(new NbSupplier5<SerializationCache>() {
        @Override
        public SerializationCache get() {
            return new SharedTypesSerializationCache(File.class);
        }
    });

    public static SerializationCache getDefault() {
        return DEFAULT_REF.get();
    }

    private SerializationCaches() {
        throw new AssertionError();
    }
}
