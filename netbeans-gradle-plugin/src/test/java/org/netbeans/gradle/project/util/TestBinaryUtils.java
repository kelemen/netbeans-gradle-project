package org.netbeans.gradle.project.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class TestBinaryUtils {
    public static void createTestJar(File dest) throws IOException {
        try (OutputStream fileOutput = new FileOutputStream(dest);
                OutputStream bufferedOutput = new BufferedOutputStream(fileOutput, 32 * 1024);
                ZipOutputStream zipOutput = new ZipOutputStream(bufferedOutput)) {
            addToZipFile(zipOutput, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n", StandardCharsets.ISO_8859_1);
        }
    }

    public static void createTestZip(File dest) throws IOException {
        try (OutputStream fileOutput = new FileOutputStream(dest);
                OutputStream bufferedOutput = new BufferedOutputStream(fileOutput, 32 * 1024);
                ZipOutputStream zipOutput = new ZipOutputStream(bufferedOutput)) {
            addToZipFile(zipOutput, "TEST-ZIP", "USED FOR TESTS", StandardCharsets.UTF_8);
        }
    }

    private static void addToZipFile(ZipOutputStream output, String entryName, String content, Charset encoding) throws IOException {
        addToZipFile(output, entryName, content.getBytes(encoding));
    }

    private static void addToZipFile(ZipOutputStream output, String entryName, byte[] content) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        output.putNextEntry(zipEntry);
        output.write(content);
        output.closeEntry();
    }

    private TestBinaryUtils() {
        throw new AssertionError();
    }
}
