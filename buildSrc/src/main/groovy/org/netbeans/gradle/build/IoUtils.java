package org.netbeans.gradle.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public final class IoUtils {
    public static void copyReader(Reader src, Writer dest) throws IOException {
        char[] buffer = new char[32 * 1024];

        while (true) {
            int readCount = src.read(buffer);
            if (readCount <= 0) {
                break;
            }

            dest.write(buffer, 0, readCount);
        }
    }

    public static void copyStream(InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[32 * 1024];

        while (true) {
            int readCount = src.read(buffer);
            if (readCount <= 0) {
                break;
            }

            dest.write(buffer, 0, readCount);
        }
    }

    private IoUtils() {
        throw new AssertionError();
    }
}
