package org.netbeans.gradle.project.output;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.Exceptions;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class InputOutputWrapper implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(InputOutputWrapper.class.getName());

    private final InputOutput io;

    private final AtomicBoolean closed;

    private final Lock ioLock;
    private volatile OutputWriter out;
    private volatile OutputWriter err;
    private volatile Reader in;

    public InputOutputWrapper(InputOutput io) {
        ExceptionHelper.checkNotNullArgument(io, "io");

        this.io = io;
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
                if (toThrow == null) {
                    toThrow = ex;
                }
                else {
                    LOGGER.log(Level.INFO, "Suppressing exception", ex);
                }
            }
        }

        if (toThrow != null) {
            if (toThrow instanceof IOException) {
                throw (IOException)toThrow;
            }

            throw Exceptions.throwUnchecked(toThrow);
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
