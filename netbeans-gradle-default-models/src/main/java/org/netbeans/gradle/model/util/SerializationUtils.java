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

    public static Object deserializeFile(File file) throws IOException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(file));
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

    public static Object deserializeObject(byte[] serializedObject) throws ClassNotFoundException {
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serializedObject));
            return input.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Object deserializeObject(
            byte[] serializedObject,
            ClassLoader classLoader) throws ClassNotFoundException {

        try {
            CustomClassObjectInputStream input = new CustomClassObjectInputStream(
                    classLoader,
                    new ByteArrayInputStream(serializedObject));

            return input.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class CustomClassObjectInputStream extends ObjectInputStream {
        private final ClassLoader classLoader;

        public CustomClassObjectInputStream(ClassLoader classLoader, InputStream input) throws IOException {
            super(input);

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
