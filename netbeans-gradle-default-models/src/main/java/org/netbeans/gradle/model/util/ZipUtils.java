package org.netbeans.gradle.model.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipUtils {
    public static File unzipResourceToTemp(Class<?> resourceBase, String resourceRelPath) throws IOException {
        return unzipResourceToTemp(resourceRelPath.startsWith("/")
                ? resourceRelPath
                : "/" + resourceBase.getPackage().getName().replace('.', '/') + "/" + resourceRelPath);
    }

    public static File unzipResourceToTemp(String resourcePath) throws IOException {
        File tempFolder = File.createTempFile("gradle-dyn-model-test", "");
        if (!tempFolder.delete()) {
            throw new IOException("Failed to remove " + tempFolder);
        }
        if (!tempFolder.mkdir()) {
            throw new IOException("Failed to create " + tempFolder);
        }

        try {
            unzipResource(resourcePath, tempFolder);
            return tempFolder;
        } catch (IOException ex) {
            recursiveDelete(tempFolder);
            throw ex;
        } catch (Throwable ex) {
            recursiveDelete(tempFolder);
            throw new RuntimeException(ex);
        }
    }

    public static void unzipResource(String resourcePath, File destDir) throws IOException {
        if (resourcePath == null) throw new NullPointerException("resourcePath");
        if (destDir == null) throw new NullPointerException("destDir");

        URL resourceURL = ZipUtils.class.getResource(resourcePath);
        if (resourceURL == null) {
            throw new IOException("Cannot find URL for resource: " + resourcePath);
        }

        InputStream input = resourceURL.openStream();
        try {
            unzip(input, destDir);
        } finally {
            input.close();
        }
    }

    private static File getEntryOutputPath(File baseDir, ZipEntry entry) {
        String name = entry.getName().replace("/", File.separator);
        return new File(baseDir, name);
    }

    public static void unzip(InputStream input, File destDir) throws IOException {
        if (input == null) throw new NullPointerException("input");
        if (destDir == null) throw new NullPointerException("destDir");

        final int BUFFER = 2048;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    File dir = new File(destDir, entry.getName());
                    if (!dir.mkdir()) {
                        throw new IOException("Failed to create directory " + dir);
                    }
                } else {
                    int count;
                    byte contents[] = new byte[BUFFER];
                    // write the files to the disk
                    FileOutputStream fos = new FileOutputStream(getEntryOutputPath(destDir, entry));
                    try {
                        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                        try {
                            while ((count = zis.read(contents, 0, BUFFER)) != -1) {
                                dest.write(contents, 0, count);
                            }
                        } finally {
                            dest.close();
                        }
                    } finally {
                        fos.close();
                    }
                }
            }
        } finally {
            zis.close();
        }
    }

    public static void recursiveDelete(File file) throws IOException {
        if (file == null) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to remove " + file);
        }
    }

    private ZipUtils() {
        throw new AssertionError();
    }
}
