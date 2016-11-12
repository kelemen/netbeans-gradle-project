package org.netbeans.gradle.model.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class SerializationUtilsTest {
    @Test
    public void testDeserializeToSameObject() throws Exception {
        Map<Object, File> src = new HashMap<Object, File>();
        src.put(1, new File("TestFile.txt"));
        src.put(2, new File("TestFile.txt"));

        byte[] serialized = SerializationUtils.serializeObject(src);

        Map<?, ?> deserialized = (Map<?, ?>)SerializationUtils.deserializeObject(serialized, SerializationCaches.getDefault());

        assertSame("File references must be the same", deserialized.get(1), deserialized.get(2));
    }

    @Test
    public void testDeserializDifferentObjects() throws Exception {
        Map<Object, File> src = new HashMap<Object, File>();

        File file1 = new File("TestFile1.txt");
        File file2 = new File("TestFile2.txt");

        src.put(1, file1);
        src.put(2, file2);

        byte[] serialized = SerializationUtils.serializeObject(src);

        Map<?, ?> deserialized = (Map<?, ?>)SerializationUtils.deserializeObject(serialized, SerializationCaches.getDefault());

        assertEquals("file1", file1, deserialized.get(1));
        assertEquals("file2", file2, deserialized.get(2));
    }
}
