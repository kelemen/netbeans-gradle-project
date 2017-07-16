package org.netbeans.gradle.project.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jtrim.cancel.Cancellation;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.junit.Assert.*;

public class NbFileUtilsTest {
    private static void addDummyFile(Path directory, String fileName, int size) throws IOException {
        byte[] content = new byte[size];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte)i;
        }

        Files.createDirectories(directory);
        Files.write(directory.resolve(fileName), content);
    }

    private static Path createDummyDirectory() throws IOException {
        Path tmpDir = Files.createTempDirectory("nb-delete-test");
        try {
            addDummyFile(tmpDir, "file1.bin", 128);
            addDummyFile(tmpDir, "file2.bin", 7);

            Path subDir = tmpDir.resolve("subdir");
            addDummyFile(subDir, "sub_file1.bin", 9);

            Path subSubDir = subDir.resolve("subsubdir");
            addDummyFile(subSubDir, "sub_sub_file1.bin", 51);
            addDummyFile(subSubDir, "sub_sub_file2.bin", 17);
            return tmpDir;
        } catch (Throwable ex) {
            NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, tmpDir);
            throw ex;
        }
    }

    @Test
    public void testDeleteDirectoryAsPath() throws IOException {
        Path dummyDir = createDummyDirectory();
        try {
            assertTrue(Files.isDirectory(dummyDir));
        } finally {
            NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, dummyDir);
        }

        assertFalse("Directory must have been removed.", Files.exists(dummyDir));
    }

    @Test
    public void testDeleteDirectoryAsFileObject() throws IOException {
        Path dummyDir = createDummyDirectory();
        try {
            assertTrue(Files.isDirectory(dummyDir));
        } finally {
            FileObject dummyDirAsObj = FileUtil.toFileObject(FileUtil.normalizeFile(dummyDir.toFile()));
            NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, dummyDirAsObj);
        }

        assertFalse("Directory must have been removed.", Files.exists(dummyDir));
    }

    private static void writeUtf8(Path dest, String text) throws IOException {
        Files.write(dest, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String generateString(int prefixLength, String suffix) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < prefixLength; i++) {
            char ch = (char)('a' + (i % ('z' - 'a' + 1)));
            result.append(ch);
        }
        result.append(suffix);
        return result.toString();
    }

    private void testTryGetLineSeparatorForTextFile(String content, String expectedLineEnding) throws IOException {
        Path dummyDir = Files.createTempDirectory("nb-f-utils-test");
        try {
            Path testFile = dummyDir.resolve("test.txt");
            writeUtf8(testFile, content);
            String chosenLineSeparator = NbFileUtils.tryGetLineSeparatorForTextFile(testFile);
            assertEquals("LineSeparator", expectedLineEnding, chosenLineSeparator);
        } finally {
            NbFileUtils.deleteDirectory(Cancellation.UNCANCELABLE_TOKEN, dummyDir);
        }
    }

    private void testTryGetLineSeparatorForTextFile(String content) throws IOException {
        testTryGetLineSeparatorForTextFile(content, "\n");
        testTryGetLineSeparatorForTextFile(content.replace("\n", "\r\n"), "\r\n");
        testTryGetLineSeparatorForTextFile(content.replace('\n', '\r'), "\r");
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileLongLine() throws IOException {
        testTryGetLineSeparatorForTextFile(generateString(16 * 1024, "\nsecond line"));
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileAfterBuffer() throws IOException {
        testTryGetLineSeparatorForTextFile(generateString(8 * 1024, "\nsecond line"));
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileBeforeBuffer() throws IOException {
        testTryGetLineSeparatorForTextFile(generateString(8 * 1024 - 1, "\nsecond line"));
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileTrailingSeperator() throws IOException {
        testTryGetLineSeparatorForTextFile("first line\n");
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileOnlySeperator() throws IOException {
        testTryGetLineSeparatorForTextFile("\n");
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileNoSeperator() throws IOException {
        testTryGetLineSeparatorForTextFile("first line", null);
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextFileEmpty() throws IOException {
        testTryGetLineSeparatorForTextFile("", null);
    }

    @Test(timeout = 5000)
    public void testTryGetLineSeparatorForTextStrangeSeparator() throws IOException {
        testTryGetLineSeparatorForTextFile("first line\n\rsecond line", null);
    }
}
