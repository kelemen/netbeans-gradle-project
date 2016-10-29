package org.netbeans.gradle.model.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

public final class SerializationUtils {
    public static ObjectInputStream newCachedObjectInputStream(
            InputStream input,
            SerializationCache cache) throws IOException {
        return new CachedObjectInputStream(input, cache);
    }

    public static ObjectInputStream newCachedObjectInputStream(
            InputStream input,
            SerializationCache cache,
            ClassLoader classLoader) throws IOException {
        return new CustomClassObjectInputStream(input, cache, classLoader);
    }

    public static byte[] serializeObject(Object object) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(2048);

        try {
            ObjectOutputStream objOutput = new ObjectOutputStream(output);
            objOutput.writeObject(object);
            objOutput.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return output.toByteArray();
    }

    public static void serializeToFile(File file, Object object) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
        try {
            output.writeObject(object);
        } finally {
            output.close();
        }
    }

    public static Object deserializeFile(File file, SerializationCache cache) throws IOException {
        ObjectInputStream input = newCachedObjectInputStream(new FileInputStream(file), cache);
        try {
            return input.readObject();
        } catch (ClassNotFoundException ex) {
            IOException toThrow = new IOException();
            toThrow.initCause(ex);
            throw toThrow;
        } finally {
            input.close();
        }
    }

    public static Object deserializeObject(byte[] serializedObject, SerializationCache cache) throws ClassNotFoundException {
        try {
            ObjectInputStream input = newCachedObjectInputStream(new ByteArrayInputStream(serializedObject), cache);
            return input.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Object deserializeObject(
            byte[] serializedObject,
            SerializationCache cache,
            ClassLoader classLoader) throws ClassNotFoundException {

        try {
            ObjectInputStream input = newCachedObjectInputStream(
                    new ByteArrayInputStream(serializedObject),
                    cache,
                    classLoader);

            return input.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class CachedObjectInputStream extends ObjectInputStream {
        private final SerializationCache cache;

        private CachedObjectInputStream(InputStream in, SerializationCache cache) throws IOException {
            super(in);

            if (cache == null) throw new NullPointerException("cache");
            this.cache = cache;

            enableResolveObject(true);
        }

        @Override
        protected final boolean enableResolveObject(boolean enable) throws SecurityException {
            // This method was declared to disable the warning: virtual method is called from the constructor.
            return super.enableResolveObject(enable);
        }

        @Override
        protected Object resolveObject(Object obj) throws IOException {
            return cache.getCached(obj);
        }
    }

    private static final class CustomClassObjectInputStream extends CachedObjectInputStream {
        private final ClassLoader classLoader;

        public CustomClassObjectInputStream(
                InputStream input,
                SerializationCache cache,
                ClassLoader classLoader) throws IOException {
            super(input, cache);

            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), false, classLoader);
            } catch (ClassNotFoundException ex) {
                // Needed for primitive types
                return super.resolveClass(desc);
            }
        }
    }

    private SerializationUtils() {
        throw new AssertionError();
    }
}
