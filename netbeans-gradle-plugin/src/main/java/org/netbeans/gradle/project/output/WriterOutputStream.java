package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

public final class WriterOutputStream extends OutputStream {
    private final Writer writer;
    private final Charset encoding;

    public WriterOutputStream(Writer writer, Charset encoding) {
        if (writer == null)
            throw new NullPointerException("writer");
        if (encoding == null)
            throw new NullPointerException("encoding");
        this.writer = writer;
        this.encoding = encoding;
    }

    public WriterOutputStream(Writer writer) {
        this(writer, Charset.defaultCharset());
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        writer.write(new String(b, encoding));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writer.write(new String(b, off, len, encoding));
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b});
    }
}
