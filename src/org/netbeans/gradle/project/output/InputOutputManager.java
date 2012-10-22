package org.netbeans.gradle.project.output;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public final class InputOutputManager {
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
            if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            if (ex instanceof Error) {
                throw (Error)ex;
            }
            throw new RuntimeException(ex.getMessage(), ex);
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

    public static class IORef implements Closeable {
        private final String name;
        private final InputOutput io;
        private final AtomicBoolean closed;

        private IORef(String name, InputOutput io) {
            if (name == null) throw new NullPointerException("name");
            if (io == null) throw new NullPointerException("io");

            this.name = name;
            this.io = io;
            this.closed = new AtomicBoolean(false);
        }

        public InputOutput getIo() {
            return io;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                decUseCount(name);
            }
        }
    }
}
