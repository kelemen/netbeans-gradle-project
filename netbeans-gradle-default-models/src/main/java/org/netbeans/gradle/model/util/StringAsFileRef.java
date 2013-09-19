package org.netbeans.gradle.model.util;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

// This class assumes that files in NB_GRADLE_TEMP_DIR are only used by this class.
// An external agent is allowed to delete the files if it can.
public final class StringAsFileRef implements Closeable {
    private static final String HEX_TABLE = "0123456789abcdef";

    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File NB_GRADLE_TEMP_DIR = new File(new File(TEMP_DIR, "nb-gradle-plugin"), "str-files");

    private final File file;
    private final RandomAccessFile fileRef;

    private static StringAsFileRef tryCreateRef(String fileName, byte[] contentBytes) throws IOException {
        if (!NB_GRADLE_TEMP_DIR.mkdirs()) {
            if (!NB_GRADLE_TEMP_DIR.isDirectory()) {
                throw new IOException("The directory to store temporary files cannot be created " + NB_GRADLE_TEMP_DIR);
            }
        }

        File file = new File(NB_GRADLE_TEMP_DIR, fileName);

        if (file.createNewFile()) {
            RandomAccessFile fileRef = null;
            boolean consumedRef = false;
            try {
                fileRef = new RandomAccessFile(file, "rw");
                fileRef.write(contentBytes);

                consumedRef = true;
                return new StringAsFileRef(file, fileRef);
            } finally {
                if (fileRef != null && !consumedRef) {
                    fileRef.close();
                }
            }
        }
        else {
            RandomAccessFile fileRef = null;
            boolean consumedRef = false;
            try {
                fileRef = new RandomAccessFile(file, "rw");

                boolean sameFile;
                if (contentBytes.length == 0) {
                    sameFile = fileRef.read() < 0;
                }
                else {
                    byte[] actualContent = new byte[contentBytes.length];
                    try {
                        fileRef.readFully(actualContent);
                        sameFile = Arrays.equals(actualContent, contentBytes) && fileRef.read() < 0;
                    } catch (EOFException ex) {
                        sameFile = false;
                    }
                }

                if (sameFile) {
                    consumedRef = true;
                    return new StringAsFileRef(file, fileRef);
                }
            } finally {
                if (fileRef != null && !consumedRef) {
                    fileRef.close();
                }
            }
        }

        return null;
    }

    public static StringAsFileRef createRef(String name, String content, Charset encoding) throws IOException {
        if (name == null) throw new NullPointerException("name");
        if (content == null) throw new NullPointerException("content");
        if (encoding == null) throw new NullPointerException("encoding");

        byte[] contentBytes = content.getBytes(encoding.name());
        String md5 = getMD5(contentBytes);

        String fileName = name + "-" + md5;
        StringAsFileRef result = tryCreateRef(fileName, contentBytes);

        int index = 0;
        while (result == null) {
            fileName = name + "-" + md5 + "-" + index;
            result = tryCreateRef(fileName, contentBytes);
            // We have tried 2^32 options (this is highly unlikely though)
            if (index == -1) {
                break;
            }

            index++;
        }

        if (result == null) {
            throw new IOException("Failed to create the file after numerous attempts.");
        }
        return result;
    }


    private static String getMD5(byte[] input) {
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

    private StringAsFileRef(File file, RandomAccessFile fileRef) {
        this.file = file;
        this.fileRef = fileRef;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "FileReference{" + file + "}";
    }

    public void close() throws IOException {
        fileRef.close();

        // TODO: Technically, this is a memory leak.
        file.deleteOnExit();
    }
}
