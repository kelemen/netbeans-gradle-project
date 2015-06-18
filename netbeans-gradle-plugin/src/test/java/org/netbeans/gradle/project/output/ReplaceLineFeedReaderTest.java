package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReplaceLineFeedReaderTest {
    private static ReplaceLineFeedReader newCrLFReader(String text, String lineSeparator) {
        return new ReplaceLineFeedReader(new StringReader(text), lineSeparator);
    }

    private static String readString(String text, String lineSeparator, int bufferOffset, int... bufferLengths) throws IOException {
        ReplaceLineFeedReader reader = newCrLFReader(text, lineSeparator);

        StringWriter result = new StringWriter(2 * text.length());

        int bufferIndex = 0;
        char[] buffer = null;
        while (true) {
            int requiredBufferLength = bufferLengths[bufferIndex] + bufferOffset;
            bufferIndex = (bufferIndex + 1) % bufferLengths.length;

            if (buffer == null || buffer.length != requiredBufferLength) {
                buffer = new char[requiredBufferLength];
            }

            int readCount = reader.read(buffer, bufferOffset, buffer.length - bufferOffset);
            if (readCount < 0) {
                break;
            }

            result.write(buffer, bufferOffset, readCount);
        }

        assertEquals("Must consitently return EOF", -1, reader.read());

        return result.toString();
    }

    private static void testReadString(String text, String lineSeparator, int bufferOffset, int... bufferLengths) throws IOException {
        String output = readString(text, lineSeparator, bufferOffset, bufferLengths);
        assertEquals(text.replace("\n", lineSeparator), output);
    }

    private static void testReadStringWithConstBuffer(String text, String lineSeparator) throws IOException {
        testReadString(text, lineSeparator, 0, 1);
        testReadString(text, lineSeparator, 0, 2);
        testReadString(text, lineSeparator, 0, 3);
        testReadString(text, lineSeparator, 0, 1024);

        testReadString(text, lineSeparator, 1, 1);
        testReadString(text, lineSeparator, 1, 2);
        testReadString(text, lineSeparator, 1, 3);
        testReadString(text, lineSeparator, 1, 1024);
    }

    private static void testReadStringWithConstBuffer(String text) throws IOException {
        testReadStringWithConstBuffer(text, "\r");
        testReadStringWithConstBuffer(text, "\r\n");
    }

    @Test
    public void testEmptyIput() throws Exception {
        testReadStringWithConstBuffer("");
    }

    @Test
    public void testSingleLf() throws Exception {
        testReadStringWithConstBuffer("\n");
    }

    @Test
    public void testLong() throws Exception {
        testReadStringWithConstBuffer("First line\nSecond Line\nThird Line");
    }

    @Test
    public void testEmptyLines() throws Exception {
        testReadStringWithConstBuffer("First line\n\n\n\nEND\n\n");
    }

    @Test
    public void testDoNotReplaceCrLf() throws Exception {
        String text = "\r\nFirst Line\r\nSecond Line\nThird Line\r\n";
        String lineSeparator = "\r\n";
        String output = readString(text, lineSeparator, 0, 1);
        assertEquals("\r\nFirst Line\r\nSecond Line\r\nThird Line\r\n", output);
    }
}
