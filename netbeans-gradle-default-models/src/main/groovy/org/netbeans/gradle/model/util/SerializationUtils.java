package org.netbeans.gradle.model.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

    public static Object deserializeObject(byte[] serializedObject) throws ClassNotFoundException {
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serializedObject));
            return input.readObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private SerializationUtils() {
        throw new AssertionError();
    }
}
