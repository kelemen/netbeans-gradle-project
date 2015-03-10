package org.netbeans.gradle.project.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SerializationUtils2 {
    public static void serializeToFile(Path file, Object object) throws IOException {
        try (OutputStream fileOutput = Files.newOutputStream(file);
                ObjectOutputStream output = new ObjectOutputStream(fileOutput)) {
            output.writeObject(object);
        }
    }

    public static Object deserializeFile(Path file) throws IOException {
        try (InputStream fileInput = Files.newInputStream(file);
                ObjectInputStream input = new ObjectInputStream(fileInput)) {
            return input.readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }

    private SerializationUtils2() {
        throw new AssertionError();
    }
}
