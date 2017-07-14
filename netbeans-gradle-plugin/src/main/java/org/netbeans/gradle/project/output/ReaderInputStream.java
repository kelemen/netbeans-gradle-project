package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.utils.ExceptionHelper;

public final class ReaderInputStream extends InputStream {
    private final Reader reader;
    private final AtomicReference<byte[]> cacheRef;

    private final Lock encoderLock;
    private final CharsetEncoder encoder;
    private boolean eofReached;
    private char[] remainingChars;

    public ReaderInputStream(Reader reader) {
        this(reader, Charset.defaultCharset());
    }

    public ReaderInputStream(Reader reader, Charset encoding) {
        Objects.requireNonNull(encoding, "encoding");

        this.reader = Objects.requireNonNull(reader, "reader");
        this.encoderLock = new ReentrantLock();
        this.encoder = encoding.newEncoder();
        this.remainingChars = null;
        this.eofReached = false;
        this.cacheRef = new AtomicReference<>(new byte[0]);
    }

    private int readFromCache(byte[] b, int offset, int length) {
        byte[] cache;
        byte[] newCache;
        int toRead;
        do {
            cache = cacheRef.get();
            toRead = Math.min(cache.length, length);
            System.arraycopy(cache, 0, b, offset, toRead);
            newCache = new byte[cache.length - toRead];
            System.arraycopy(cache, toRead, newCache, 0, newCache.length);
        } while (!cacheRef.compareAndSet(cache, newCache));
        return toRead;
    }

    private ByteBuffer encodeChars(char[] chars, int charCount) throws CharacterCodingException {
        return encodeChars(chars, charCount, false);
    }

    private ByteBuffer encodeChars(char[] chars, int charCount, boolean finalBytes) throws CharacterCodingException {
        encoderLock.lock();
        try {
            CharBuffer input;
            if (remainingChars == null || remainingChars.length == 0) {
                input = CharBuffer.wrap(chars, 0, charCount);
            }
            else {
                char[] toPrepend = remainingChars;
                remainingChars = null;

                char[] newChars = new char[charCount + toPrepend.length];
                System.arraycopy(toPrepend, 0, newChars, 0, toPrepend.length);
                System.arraycopy(chars, 0, newChars, toPrepend.length, charCount);

                input = CharBuffer.wrap(newChars);
            }

            int n = (int)(input.remaining() * encoder.averageBytesPerChar());
            ByteBuffer out = ByteBuffer.allocate(n);

            while (true) {
                CoderResult result = encoder.encode(input, out, finalBytes);
                if (result.isOverflow()) {
                    n = 2 * n + 1;
                    ByteBuffer newOut = ByteBuffer.allocate(n);
                    out.flip();
                    newOut.put(out);
                    out = newOut;
                }
                else if (result.isError()) {
                    result.throwException();
                }
                else {
                    int remainingCount = input.remaining();
                    if (remainingCount > 0) {
                        char[] currentRemaining = new char[remainingCount];
                        input.get(currentRemaining);
                        remainingChars = currentRemaining;
                    }
                    break;
                }
            }

            out.flip();
            return out;
        } finally {
            encoderLock.unlock();
        }
    }

    private ByteBuffer completeStream() throws CharacterCodingException {
        encoderLock.lock();
        try {
            if (eofReached) {
                return null;
            }
            eofReached = true;

            char[] finalChars = remainingChars;
            remainingChars = null;

            if (finalChars == null) {
                finalChars = new char[0];
            }

            ByteBuffer out = encodeChars(finalChars, finalChars.length, true);
            int n = out.capacity();

            while (true) {
                CoderResult result = encoder.flush(out);
                if (result.isUnderflow()) {
                    break;
                }

                if (result.isOverflow()) {
                    n = 2 * n + 1;
                    ByteBuffer newOut = ByteBuffer.allocate(n);
                    out.flip();
                    newOut.put(out);
                    out = newOut;
                }
                else {
                    result.throwException();
                }
            }

            out.flip();
            return out;
        } finally {
            encoderLock.unlock();
        }
    }

    private boolean readToCache(int requiredBytes) throws IOException {
        assert requiredBytes > 0;
        // We rely on the encoder to choose the number of bytes to read but
        // it does not have to be actually accurate, it only matters
        // performance wise but this is not a performance critical code.
        int toRead = (int)((float)requiredBytes / encoder.averageBytesPerChar()) + 1;
        toRead = Math.max(toRead, requiredBytes);
        char[] readChars = new char[toRead];
        int readCount = reader.read(readChars);

        ByteBuffer encodedBuffer;
        if (readCount <= 0) {
            // readCount should never be zero but if reader returns zero
            // regardless, assume that it believes that EOF has been
            // reached.

            encodedBuffer = completeStream();
            if (encodedBuffer == null || encodedBuffer.remaining() <= 0) {
                return false;
            }
        }
        else {
            encodedBuffer = encodeChars(readChars, readCount);
        }

        byte[] encoded = new byte[encodedBuffer.remaining()];
        encodedBuffer.get(encoded);
        appendToCache(encoded);
        return true;
    }

    private void appendToCache(byte[] newBytes) {
        cacheRef.updateAndGet(oldCache -> {
            byte[] newCache = new byte[oldCache.length + newBytes.length];
            System.arraycopy(oldCache, 0, newCache, 0, oldCache.length);
            System.arraycopy(newBytes, 0, newCache, oldCache.length, newBytes.length);
            return newCache;
        });
    }

    @Override
    public int read() throws IOException {
        byte[] result = new byte[1];
        if (read(result) <= 0) {
            // Althouth the above read should never return zero.
            return -1;
        }
        else {
            return (int)result[0] & 0xFF;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b, "b");
        ExceptionHelper.checkArgumentInRange(off, 0, b.length, "off");
        ExceptionHelper.checkArgumentInRange(len, 0, b.length - off, "len");
        // Note that while this method is implemented to be thread-safe
        // calling it concurrently is unadvised, since read is not atomic.

        if (len == 0) {
            return 0;
        }

        int currentOffset = off;
        int currentLength = len;
        int readCount = 0;
        do {
            int currentRead = readFromCache(b, currentOffset, currentLength);
            readCount += currentRead;
            currentOffset += currentRead;
            currentLength -= currentRead;
            if (readCount > 0) {
                return readCount;
            }
        } while (readToCache(currentLength));
        return readCount > 0 ? readCount : -1;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
