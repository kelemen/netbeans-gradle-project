package org.netbeans.gradle.project;

import org.netbeans.gradle.model.util.StringAsFileRef;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringAsFileRefTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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

    private static void assertContent(StringAsFileRef fileRef, String expectedContent, Charset encoding) throws IOException {
        String fileContent = new String(readAll(fileRef.getFile()), UTF8);
        assertEquals(expectedContent, fileContent);
    }

    private void testFileContainsText(String name, String content) throws Exception {
        StringAsFileRef fileRef = StringAsFileRef.createRef(name, content, UTF8);
        try {
            assertContent(fileRef, content, UTF8);
        } finally {
            fileRef.close();
        }
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
    public void testMultipleRefs() throws Exception {
        String content = "EXPECTED FILE content: testMultipleRefs";

        String name = "testMultipleRefs";
        StringAsFileRef fileRef1 = StringAsFileRef.createRef(name, content, UTF8);
        try {
            StringAsFileRef fileRef2 = StringAsFileRef.createRef(name, content, UTF8);
            try {
                assertEquals(fileRef1.getFile(), fileRef2.getFile());

                assertContent(fileRef1, content, UTF8);
            } finally {
                fileRef2.close();
            }
        } finally {
            fileRef1.close();
        }
    }

    private void testModifiedContent(String name, byte[] modContent) throws Exception {
        String content = "EXPECTED FILE content: testModifiedContent";

        StringAsFileRef fileRef1 = StringAsFileRef.createRef(name, content, UTF8);
        fileRef1.close();

        RandomAccessFile fileContent = new RandomAccessFile(fileRef1.getFile(), "rw");
        try {
            fileContent.setLength(0);
            fileContent.write(modContent);
        } finally {
            fileContent.close();
        }

        StringAsFileRef fileRef2 = StringAsFileRef.createRef(name, content, UTF8);
        try {
            assertContent(fileRef2, content, UTF8);
        } finally {
            fileRef2.close();
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
