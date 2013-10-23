package org.netbeans.gradle.project.output;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class InputOutputManager {
    private static final Logger LOGGER = Logger.getLogger(InputOutputManager.class.getName());

    private static final Lock MAIN_LOCK = new ReentrantLock();
    private static final Map<String, Integer> USE_COUNTS = new HashMap<String, Integer>();

    public static IORef getInputOutput(String name, boolean mayReuse, boolean mayClean) {
        String caption;
        boolean createNew;

        MAIN_LOCK.lock();
        try {
            Integer counter = USE_COUNTS.get(name);
            if (counter == null) counter = 0;

            if (mayReuse) {
                caption = name;
                createNew = false;
            }
            else {
                caption = counter.intValue() == 0
                    ? name
                    : name + " #" + (counter + 1);

                createNew = !mayClean;
            }

            USE_COUNTS.put(name, counter + 1);
        } finally {
            MAIN_LOCK.unlock();
        }

        try {
            return new IORef(name, IOProvider.getDefault().getIO(caption, createNew));
        } catch (Throwable ex) {
            decUseCount(caption);
            throw Exceptions.throwUnchecked(ex);
        }
    }

    private static void decUseCount(String name) {
        MAIN_LOCK.lock();
        try {
            Integer counter = USE_COUNTS.get(name);
            if (counter != null) {
                if (counter.intValue() > 1) {
                    USE_COUNTS.put(name, counter - 1);
                }
                else {
                    USE_COUNTS.remove(name);
                }
            }
        } finally {
            MAIN_LOCK.unlock();
        }
    }

    public static final class IORef implements Closeable {
        private final String name;
        private final InputOutput io;
        private final AtomicBoolean closed;

        private final Lock ioLock;
        private volatile OutputWriter out;
        private volatile OutputWriter err;
        private volatile Reader in;

        private IORef(String name, InputOutput io) {
            if (name == null) throw new NullPointerException("name");
            if (io == null) throw new NullPointerException("io");

            this.name = name;
            this.io = io;
            this.closed = new AtomicBoolean(false);

            this.ioLock = new ReentrantLock();
            this.out = null;
            this.err = null;
            this.in = null;
        }

        private void checkNotClosed() {
            if (closed.get()) {
                throw new IllegalStateException();
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
                else if (toThrow instanceof Error) {
                    throw (Error)toThrow;
                }
                else if (toThrow instanceof RuntimeException) {
                    throw (RuntimeException)toThrow;
                }
                else {
                    throw new RuntimeException(toThrow);
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                decUseCount(name);

                ioLock.lock();
                try {
                    closeAll(in, out, err);
                } finally {
                    ioLock.unlock();
                }
            }
        }
    }
}
