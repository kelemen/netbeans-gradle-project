package org.netbeans.gradle.model.util;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BasicFileUtils {
    private static final Logger LOGGER = Logger.getLogger(BasicFileUtils.class.getName());

    private static final String HEX_TABLE = "0123456789abcdef";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File NB_GRADLE_TEMP_DIR = toCanonicalFile(getSubPath(TEMP_DIR, "nb-gradle-plugin"));
    private static final int MAX_TMP_FILE_WITH_SAME_NAME = 5;

    public static File getPluginTmpDir() {
        return NB_GRADLE_TEMP_DIR;
    }

    public static File createTmpFile(String preferredName, String suffix) throws IOException {
        if (!NB_GRADLE_TEMP_DIR.isDirectory()) {
            if (!NB_GRADLE_TEMP_DIR.mkdirs()) {
                LOGGER.log(Level.WARNING, "Could not create directory: {0}", NB_GRADLE_TEMP_DIR);
            }
        }

        File result = getSubPath(NB_GRADLE_TEMP_DIR, preferredName + suffix);
        if (result.createNewFile()) {
            return result;
        }

        for (int i = 1; i <= MAX_TMP_FILE_WITH_SAME_NAME; i++) {
            result = getSubPath(NB_GRADLE_TEMP_DIR, preferredName + i + suffix);
            if (result.createNewFile()) {
                return result;
            }
        }

        return File.createTempFile(preferredName, suffix, NB_GRADLE_TEMP_DIR);
    }

    public static File getSubPath(File root, String... subPaths) {
        File result = root;
        for (String subprojectName: subPaths) {
            result = new File(result, subprojectName);
        }
        return result;
    }

    public static File toCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file;
        }
    }

    public static String getMD5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Hash = md.digest(input);
            return byteArrayToHex(md5Hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing MD5 MessageDigest");
        }
    }

    private static String byteArrayToHex(byte[] array) {
        StringBuilder result = new StringBuilder(array.length * 2);
        for (byte value: array) {
            result.append(HEX_TABLE.charAt(((int)value & 0xF0) >>> 4));
            result.append(HEX_TABLE.charAt((int)value & 0x0F));
        }
        return result.toString();
    }

    private BasicFileUtils() {
        throw new AssertionError();
    }
}
