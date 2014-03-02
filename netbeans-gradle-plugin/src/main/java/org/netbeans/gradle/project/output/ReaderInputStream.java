package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class ReaderInputStream extends InputStream {
    private final Reader reader;
    private final Charset encoding;
    private final AtomicReference<byte[]> cacheRef;

    public ReaderInputStream(Reader reader) {
        this(reader, Charset.defaultCharset());
    }

    public ReaderInputStream(Reader reader, Charset encoding) {
        ExceptionHelper.checkNotNullArgument(reader, "reader");
        ExceptionHelper.checkNotNullArgument(encoding, "encoding");

        this.reader = reader;
        this.encoding = encoding;
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

    private boolean readToCache(int requiredBytes) throws IOException {
        assert requiredBytes > 0;
        // We rely on the encoder to choose the number of bytes to read but
        // it does not have to be actually accurate, it only matters
        // performance wise but this is not a performance critical code.
        CharsetEncoder encoder = encoding.newEncoder();
        int toRead = (int)((float)requiredBytes / encoder.averageBytesPerChar()) + 1;
        toRead = Math.max(toRead, requiredBytes);
        char[] readChars = new char[toRead];
        int readCount = reader.read(readChars);
        if (readCount <= 0) {
            // readCount should never be zero but if reader returns zero
            // regardless, assume that it believes that EOF has been
            // reached.
            return false;
        }
        ByteBuffer encodedBuffer = encoder.encode(CharBuffer.wrap(readChars, 0, readCount));
        byte[] encoded = new byte[encodedBuffer.remaining()];
        encodedBuffer.get(encoded);
        byte[] oldCache;
        byte[] newCache;
        do {
            oldCache = cacheRef.get();
            newCache = new byte[oldCache.length + encoded.length];
            System.arraycopy(oldCache, 0, newCache, 0, oldCache.length);
            System.arraycopy(encoded, 0, newCache, oldCache.length, encoded.length);
        } while (!cacheRef.compareAndSet(oldCache, newCache));
        return true;
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
        ExceptionHelper.checkNotNullArgument(b, "b");
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
