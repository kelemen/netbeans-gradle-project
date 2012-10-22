package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LineOutputWriter extends Writer {
    public static interface Handler {
        public void writeLine(String line) throws IOException;
        public void flush() throws IOException;
    }

    private final Handler handler;
    private final Lock mainLock;
    private final StringBuilder lineBuffer;
    private final AtomicBoolean closed;
    private char lastChar;

    public LineOutputWriter(Handler handler) {
        if (handler == null) throw new NullPointerException("handler");

        this.handler = handler;
        this.lineBuffer = new StringBuilder(256);
        this.mainLock = new ReentrantLock();
        this.lastChar = '\0';
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (cbuf == null) throw new NullPointerException("cbuf");
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        int endOffset = off + len;
        if (off < 0 || off > cbuf.length || endOffset > cbuf.length || endOffset < 0) {
            throw new IndexOutOfBoundsException();
        }

        int currentOffset = off;
        int currentLength = len;

        while (currentLength > 0) {
            String line = null;

            mainLock.lock();
            try {
                while (currentLength > 0) {
                    char currentChar = cbuf[currentOffset];
                    char prevChar = lastChar;
                    lastChar = currentChar;

                    currentOffset++;
                    currentLength--;

                    if (prevChar == '\r' && currentChar == '\n') {
                        continue;
                    }

                    if (currentChar == '\n' || currentChar == '\r') {
                        line = lineBuffer.toString();
                        lineBuffer.setLength(0);
                        break;
                    }
                    else {
                        lineBuffer.append(currentChar);
                    }
                }
            } finally {
                mainLock.unlock();
            }

            if (line != null) {
                handler.writeLine(line);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        handler.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        String line;
        mainLock.lock();
        try {
            line = lineBuffer.toString();
            lineBuffer.setLength(0);
        } finally {
            mainLock.unlock();
        }

        handler.writeLine(line);
        handler.flush();
    }
}
