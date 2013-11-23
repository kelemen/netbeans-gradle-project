package org.netbeans.gradle.model.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import org.junit.Test;

import static org.junit.Assert.*;

public class TemporaryFileManagerTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static byte[] readAll(InputStream input) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        ByteArrayOutputStream result = new ByteArrayOutputStream(256);

        int readCount = input.read(buffer);

        while (readCount > 0) {
            result.write(buffer, 0, readCount);
            readCount = input.read(buffer);
        }

        return result.toByteArray();
    }

    private static byte[] readAll(File file) throws IOException {
        FileInputStream fileStream = new FileInputStream(file);
        try {
            return readAll(fileStream);
        } finally {
            fileStream.close();
        }
    }

    private static void assertContent(TemporaryFileRef fileRef, String expectedContent) throws IOException {
        String fileContent = new String(readAll(fileRef.getFile()), UTF8.name());
        assertEquals(expectedContent, fileContent);
    }

    private void testFileContainsText(String name, String content) throws Exception {
        TemporaryFileRef fileRef = createRef(name, content);
        try {
            assertContent(fileRef, content);
        } finally {
            fileRef.close();
        }
    }

    private TemporaryFileRef createRef(String name, String content) throws IOException {
        return TemporaryFileManager.getDefault().createFile(name, content, UTF8);
    }

    @Test
    public void testFileContainsTextEmpty() throws Exception {
        testFileContainsText("testFileContainsTextEmpty", "");
    }

    @Test
    public void testFileContainsTextNonEmpty() throws Exception {
        testFileContainsText("testFileContainsTextNonEmpty", "EXPECTED FILE content: testFileContainsText");
    }

    @Test
    public void testDeleteAfterClose() throws Exception {
        String content = "EXPECTED FILE content: testMultipleRefs";

        String name = "testMultipleRefs";
        TemporaryFileRef fileRef1 = createRef(name, content);
        File file;
        try {
            file = fileRef1.getFile();
        } finally {
            fileRef1.close();
        }

        assertFalse("File must be deleted after close.", file.exists());
    }

    @Test
    public void testMultipleRefs() throws Exception {
        String content = "EXPECTED FILE content: testMultipleRefs";

        String name = "testMultipleRefs";
        TemporaryFileRef fileRef1 = createRef(name, content);
        try {
            TemporaryFileRef fileRef2 = createRef(name, content);
            try {
                assertEquals(fileRef1.getFile(), fileRef2.getFile());

                assertContent(fileRef2, content);
            } finally {
                fileRef2.close();
            }

            assertContent(fileRef1, content);
        } finally {
            fileRef1.close();
        }

        assertFalse("File must be deleted after close.", fileRef1.getFile().exists());
    }

    private void testModifiedContent(String name, byte[] modContent) throws Exception {
        String content = "EXPECTED FILE content: testModifiedContent";

        TemporaryFileRef fileRef1 = createRef(name, content);
        fileRef1.close();

        try {
            RandomAccessFile fileContent = new RandomAccessFile(fileRef1.getFile(), "rw");
            try {
                fileContent.setLength(0);
                fileContent.write(modContent);
            } finally {
                fileContent.close();
            }

            TemporaryFileRef fileRef2 = createRef(name, content);
            try {
                assertContent(fileRef2, content);
            } finally {
                fileRef2.close();
            }
        } finally {
            fileRef1.getFile().delete();
        }
    }

    @Test
    public void testModifiedContentWithEmpty() throws Exception {
        testModifiedContent("testModifiedContentWithEmpty", new byte[0]);
    }

    @Test
    public void testModifiedContentWithNonEmpty() throws Exception {
        testModifiedContent("testModifiedContentWithNonEmpty", new byte[]{1, 2, 3});
    }
}
