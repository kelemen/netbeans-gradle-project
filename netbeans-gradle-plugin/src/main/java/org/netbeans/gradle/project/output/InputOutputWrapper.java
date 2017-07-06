package org.netbeans.gradle.project.output;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.netbeans.gradle.model.util.Exceptions;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class InputOutputWrapper implements Closeable {
    private final InputOutput io;

    private final AtomicBoolean closed;

    private final Lock ioLock;
    private volatile OutputWriter out;
    private volatile OutputWriter err;
    private volatile Reader in;

    public InputOutputWrapper(InputOutput io) {
        this.io = Objects.requireNonNull(io, "io");
        this.closed = new AtomicBoolean(false);

        this.ioLock = new ReentrantLock();
        this.out = null;
        this.err = null;
        this.in = null;
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Cannot access method after I/O close.");
        }
    }

    public InputOutput getIo() {
        return io;
    }

    public OutputWriter getOutRef() {
        checkNotClosed();
        OutputWriter result = out;
        if (result == null) {
            ioLock.lock();
            try {
                result = out;
                if (result == null) {
                    result = io.getOut();
                    out = result;
                }
            } finally {
                ioLock.unlock();
            }
        }
        return result;
    }

    public OutputWriter getErrRef() {
        checkNotClosed();
        OutputWriter result = err;
        if (result == null) {
            ioLock.lock();
            try {
                result = err;
                if (result == null) {
                    result = io.getErr();
                    err = result;
                }
            } finally {
                ioLock.unlock();
            }
        }
        return result;
    }

    public Reader getInRef() {
        checkNotClosed();
        Reader result = in;
        if (result == null) {
            ioLock.lock();
            try {
                result = in;
                if (result == null) {
                    result = io.getIn();
                    in = result;
                }
            } finally {
                ioLock.unlock();
            }
        }
        return result;
    }

    private static void closeAll(Closeable... resources) throws IOException {
        Throwable toThrow = null;
        for (Closeable resource: resources) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Throwable ex) {
                if (toThrow == null) toThrow = ex;
                else toThrow.addSuppressed(ex);
            }
        }

        if (toThrow != null) {
            if (toThrow instanceof IOException) {
                throw (IOException)toThrow;
            }

            throw Exceptions.throwUnchecked(toThrow);
        }
    }

    public void closeStreamsForNow() throws IOException {
        ioLock.lock();
        try {
            closeAll(in, out, err);
        } finally {
            ioLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            ioLock.lock();
            try {
                closeAll(in, out, err);
            } finally {
                ioLock.unlock();
            }
        }
    }
}
