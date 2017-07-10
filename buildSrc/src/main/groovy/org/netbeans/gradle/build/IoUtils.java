package org.netbeans.gradle.build;

import java.io.IOException;
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

    private IoUtils() {
        throw new AssertionError();
    }
}
