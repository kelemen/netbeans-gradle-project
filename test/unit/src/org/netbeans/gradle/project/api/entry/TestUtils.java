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
    /**
     * Unzip the file
     * <code>f</code> to folder
     * <code>destDir</code>.
     *
     * @param f file to unzip
     * @param destDir destination directory
     */
    public static void unzip(InputStream is, File destDir) throws IOException {
        final int BUFFER = 2048;
        BufferedOutputStream dest;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                File dir = new File(destDir, entry.getName());
                dir.mkdir();
            } else {
                int count;
                byte contents[] = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(new File(destDir, entry.getName()));
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(contents, 0, BUFFER)) != -1) {
                    dest.write(contents, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
        zis.close();
    }
    
    public static void recursiveDelete(File file) {
        if (file == null) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        file.delete();
    }
}
