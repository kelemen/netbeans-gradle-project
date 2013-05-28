package org.netbeans.gradle.project.api.entry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author radim
 */
public class TestUtils {
    public static void unzip(InputStream is, File destDir) throws IOException {
        final int BUFFER = 2048;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
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
                    FileOutputStream fos = new FileOutputStream(new File(destDir, entry.getName()));
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
}
