package org.netbeans.gradle.project.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jtrim2.cancel.Cancellation;
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
}
