package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.netbeans.gradle.project.util.StringUtils;

public final class ReplaceLineFeedReader extends Reader {
    private final Reader src;
    private final LfReplacingBuffer buffer;
    private final Lock bufferLock;

    public ReplaceLineFeedReader(Reader src, String newLineSeparator) {
        Objects.requireNonNull(newLineSeparator, "newLineSeparator");

        this.src = Objects.requireNonNull(src, "src");
        this.buffer = new LfReplacingBuffer(newLineSeparator);
        this.bufferLock = new ReentrantLock();
    }

    public static Reader replaceLfWithOsLineSeparator(Reader src) {
        return replaceLf(src, StringUtils.getOsLineSeparator());
    }

    public static Reader replaceLf(Reader src, String newLineSeparator) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(newLineSeparator, "newLineSeparator");

        if ("\n".equals(newLineSeparator)) {
            return src;
        }
        else {
            return new ReplaceLineFeedReader(src, newLineSeparator);
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        // It is possible to optimize this method much more but there is
        // no practical reason to do so.

        int readCount = src.read(cbuf, off, len);

        bufferLock.lock();
        try {
            if (readCount > 0) {
                buffer.appendChars(cbuf, off, readCount);
            }

            if (buffer.getCharCount() <= 0) {
                assert readCount <= 0;
                return readCount;
            }

            return buffer.moveFirstTo(cbuf, off, len);
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        src.close();
    }

    private static final class LfReplacingBuffer {
        private static final char[] NO_CHARS = new char[0];

        private final char[] newLineSeparatorChars;

        private int charCount;
        private char[] chars;
        private char lastChar;

        public LfReplacingBuffer(String newLineSeparator) {
            this.newLineSeparatorChars = newLineSeparator.toCharArray();
            this.charCount = 0;
            this.chars = NO_CHARS;
            this.lastChar = '\0';
        }

        public int getCharCount() {
            return charCount;
        }

        private char[] getBuffer(int requiredExtraLength) {
            int requiredLength = charCount + requiredExtraLength;

            char[] result = chars;
            if (result.length >= requiredLength) {
                return result;
            }

            char[] newChars = new char[Math.max(requiredLength, 2 * result.length)];
            System.arraycopy(chars, 0, newChars, 0, charCount);
            chars = newChars;

            return newChars;
        }

        private int getLfCount(char[] cbuf, int off, int len) {
            int result = 0;
            for (int i = off + len - 1; i >= off; i--) {
                if (cbuf[i] == '\n') {
                    result++;
                }
            }
            return result;
        }

        public void appendChars(char[] cbuf, int off, int len) {
            int lfCount = getLfCount(cbuf, off, len);
            int extraChars = len + lfCount * (newLineSeparatorChars.length - 1);

            char[] buffer = getBuffer(extraChars);

            int destOffset = charCount;

            char prevChar = lastChar;
            int endSrcIndex = off + len;
            for (int i = off; i < endSrcIndex; i++) {
                char ch = cbuf[i];

                if (prevChar != '\r' && ch == '\n') {
                    System.arraycopy(newLineSeparatorChars, 0, buffer, destOffset, newLineSeparatorChars.length);
                    destOffset += newLineSeparatorChars.length;
                }
                else {
                    buffer[destOffset] = ch;
                    destOffset++;
                }

                prevChar = ch;
            }

            lastChar = prevChar;
            charCount = destOffset;
        }

        public int moveFirstTo(char[] cbuf, int off, int len) {
            int resultLength = Math.min(len, charCount);

            System.arraycopy(chars, 0, cbuf, off, resultLength);
            System.arraycopy(chars, resultLength, chars, 0, charCount - resultLength);
            charCount -= resultLength;

            return resultLength;
        }
    }
}
