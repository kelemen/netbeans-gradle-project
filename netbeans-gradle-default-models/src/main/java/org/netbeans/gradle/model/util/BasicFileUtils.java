package org.netbeans.gradle.model.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BasicFileUtils {
    private static final Logger LOGGER = Logger.getLogger(BasicFileUtils.class.getName());

    private static final char[] HEX_TABLE_LOWER = "0123456789abcdef".toCharArray();
    private static final char[] HEX_TABLE_UPPER = "0123456789ABCDEF".toCharArray();

    private static final AtomicReference<File> NB_GRADLE_TEMP_DIR_REF = new AtomicReference<File>(null);
    private static final int MAX_TMP_FILE_WITH_SAME_NAME = 5;

    private static int sumLengths(String... strings)  {
        int length = 0;
        for (String string: strings) {
            length += string.length();
        }
        return length;
    }

    private static void appendAsEscaped(char ch, StringBuilder result) {
        result.append('\\');
        result.append('u');

        int value = ch & 0xFFFF;
        result.append(HEX_TABLE_UPPER[(value >>> 12) & 0xF]);
        result.append(HEX_TABLE_UPPER[(value >>> 8) & 0xF]);
        result.append(HEX_TABLE_UPPER[(value >>> 4) & 0xF]);
        result.append(HEX_TABLE_UPPER[value & 0xF]);
    }

    public static String toSafelyPastableToJavaCode(String... strings)  {
        StringBuilder result = new StringBuilder(sumLengths(strings));

        char lastCh = '?'; // Anything but '\\'
        for (String string: strings) {
            for (int i = 0; i < string.length(); i++) {
                char ch = string.charAt(i);
                if ((lastCh == '\\' && ch == 'u') || ch > 127) {
                    appendAsEscaped(ch, result);
                }
                else {
                    result.append(ch);
                }

                lastCh = ch;
            }
        }

        return result.toString();
    }

    private static File findAvailableTempFileDir() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        String userName = makeSafeForFileNames(System.getProperty("user.name"));
        String defaultDirName = "nb-gradle-plugin-" + userName;

        for (int i = 0; i < MAX_TMP_FILE_WITH_SAME_NAME; i++) {
            String dirName = i > 0
                    ? defaultDirName + "-" + i
                    : defaultDirName;

            File result = getSubPath(tempDir, dirName);
            result.mkdirs();
            if (isWriteable(result)) {
                return result;
            }
        }

        SecureRandom rand = new SecureRandom();
        String part1 = Long.toHexString(rand.nextLong());
        String part2 = Long.toHexString(rand.nextLong());

        File result = getSubPath(tempDir, defaultDirName + "-" + part1 + "-" + part2);
        result.mkdirs();
        if (isWriteable(result)) {
            LOGGER.log(Level.WARNING,
                    "Using a directory with random name for temporary storage: {0}",
                    result);
            return result;
        }

        LOGGER.log(Level.WARNING,
                "Couldn't create any directory for test files. Using the temp directory: {0}",
                tempDir);
        return tempDir;
    }

    private static boolean isWriteable(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }

        File testTempFile = null;
        try {
            testTempFile = File.createTempFile("write-test", ".tmp", dir);
            if (!testTempFile.isFile()) {
                return false;
            }

            int testByte = 0xAC;

            FileOutputStream output = new FileOutputStream(testTempFile);
            try {
                output.write(testByte);
                output.flush();
            } finally {
                output.close();
            }

            FileInputStream input = new FileInputStream(testTempFile);
            try {
                if (input.read() != testByte) {
                    return false;
                }
            } finally {
                input.close();
            }

            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (testTempFile != null) {
                testTempFile.delete();
            }
        }
    }

    private static boolean isCharOkForFileName(char ch) {
        if (ch >= 'A' && ch <= 'Z') return true;
        if (ch >= 'a' && ch <= 'z') return true;
        if (ch >= '0' && ch <= '9') return true;

        return ch == '-' || ch == '_';
    }

    private static String makeSafeForFileNames(String unsafeName) {
        if (unsafeName == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(unsafeName.length());
        for (int i = 0; i < unsafeName.length(); i++) {
            char ch = unsafeName.charAt(i);
            if (isCharOkForFileName(ch)) {
                result.append(ch);
            }
            else {
                result.append('_');
            }
        }
        return result.toString();
    }

    public static File getPluginTmpDir() {
        File result = NB_GRADLE_TEMP_DIR_REF.get();
        if (result == null) {
            result = toCanonicalFile(findAvailableTempFileDir());
            NB_GRADLE_TEMP_DIR_REF.compareAndSet(null, result);
            result = NB_GRADLE_TEMP_DIR_REF.get();
        }
        return result;
    }

    public static File createTmpFile(String preferredName, String suffix) throws IOException {
        File tmpDir = getPluginTmpDir();

        if (!tmpDir.isDirectory()) {
            if (!tmpDir.mkdirs()) {
                LOGGER.log(Level.WARNING, "Could not create directory: {0}", tmpDir);
            }
        }

        File result = getSubPath(tmpDir, preferredName + suffix);
        if (result.createNewFile()) {
            return result;
        }

        for (int i = 1; i <= MAX_TMP_FILE_WITH_SAME_NAME; i++) {
            result = getSubPath(tmpDir, preferredName + i + suffix);
            if (result.createNewFile()) {
                return result;
            }
        }

        return File.createTempFile(preferredName, suffix, tmpDir);
    }

    public static File getSubPath(File root, String... subPaths) {
        File result = root;
        for (String subprojectName: subPaths) {
            result = new File(result, subprojectName);
        }
        return result;
    }

    public static File toCanonicalFile(File file) {
        if (file == null) {
            return null;
        }

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
            result.append(HEX_TABLE_LOWER[((int)value & 0xF0) >>> 4]);
            result.append(HEX_TABLE_LOWER[(int)value & 0x0F]);
        }
        return result.toString();
    }

    private BasicFileUtils() {
        throw new AssertionError();
    }
}
