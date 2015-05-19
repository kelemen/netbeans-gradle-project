package org.netbeans.gradle.project.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import org.junit.Test;
import org.netbeans.gradle.project.util.StringUtils;

import static org.junit.Assert.*;

public class ReaderInputStreamTest {
    private static final Charset[] UNIVERSAL_CHARSETS = new Charset[] {
        StringUtils.UTF8,
        Charset.forName("UTF-16LE"),
    };

    private static final Charset[] CHARSETS = new Charset[] {
        StringUtils.UTF8,
        Charset.forName("UTF-16LE"),
        Charset.forName("ISO-8859-1"),
        Charset.forName("ISO-8859-2"),
    };

    private static byte[] readAllBytes(InputStream input, int copyBatchSize) throws IOException {
        byte[] buffer = new byte[copyBatchSize];

        ByteArrayOutputStream result = new ByteArrayOutputStream(4096);
        while (true) {
            int readCount = input.read(buffer);
            if (readCount <= 0) {
                break;
            }

            result.write(buffer, 0, readCount);
        }

        return result.toByteArray();
    }

    private static void doTest(Charset charset, String text, int copyBatchSize) throws IOException {
        try {
            ReaderInputStream reader = new ReaderInputStream(new StringReader(text), charset);

            byte[] bytes = readAllBytes(reader, copyBatchSize);
            assertEquals(text, new String(bytes, charset));
        } catch (Throwable ex) {
            throw new AssertionError("Test failed for charset: " + charset.name()
                    + ", copyBatchSize: " + copyBatchSize
                    + ", text: " + text,
                    ex);
        }
    }

    private static void doTestForAllCharsets(String text) throws IOException {
        for (Charset charset: CHARSETS) {
            doTest(charset, text);
        }
    }

    private static void doTestForUniversalCharsets(String text) throws IOException {
        for (Charset charset: UNIVERSAL_CHARSETS) {
            doTest(charset, text);
        }
    }

    private static void doTest(Charset charset, String text) throws IOException {
        for (int copyBatchSize: new int[]{1, 2, 1024, 4096, 4 * text.length()}) {
            doTest(charset, text, copyBatchSize);
        }
    }

    @Test
    public void testEmpty() throws IOException {
        doTestForAllCharsets("");
    }

    @Test
    public void test1AsciiChar() throws IOException {
        doTestForAllCharsets("a");
    }

    @Test
    public void test2AsciiChars() throws IOException {
        doTestForAllCharsets("ab");
    }

    @Test
    public void test10AsciiChars() throws IOException {
        doTestForAllCharsets("abcdefghij");
    }

    private static boolean isValidSingleCodePoint(int value) {
        return value != 0xFFFE && (value < 0xD800 || value > 0xDFFF);
    }

    @Test
    public void testSingleChar() throws IOException {
        int lastTested = -1;

        try {
            int[] codePoint = new int[1];
            for (int i = 0; i < 100_000; i++) {
                if (isValidSingleCodePoint(i)) {
                    lastTested = i;
                    codePoint[0] = i;
                    doTestForUniversalCharsets(new String(codePoint, 0, 1));
                }
            }
        } catch (Throwable ex) {
            throw new AssertionError("Test failed for codepoint: " + lastTested, ex);
        }
    }

    @Test
    public void testForLongString() throws IOException {
        int charCount = 100_000;
        StringBuilder str = new StringBuilder(charCount);
        for (int i = 0; i < charCount; i++) {
            if (isValidSingleCodePoint(i)) {
                str.appendCodePoint(i);
            }
        }

        doTestForUniversalCharsets(str.toString());
    }

    @Test
    public void testForAsciiLongString() throws IOException {
        int charCount = 100_000;
        StringBuilder str = new StringBuilder(charCount);
        char currentChar = 'a';
        for (int i = 0; i < 100000; i++) {
            str.append(currentChar);
            currentChar = (char)(currentChar + 1);
            if (currentChar > 'z') {
                currentChar = 'a';
            }
        }

        doTestForAllCharsets(str.toString());
    }
}
