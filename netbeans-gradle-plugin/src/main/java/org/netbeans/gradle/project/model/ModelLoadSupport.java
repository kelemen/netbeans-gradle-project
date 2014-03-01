package org.netbeans.gradle.project.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ModelLoadSupport {
    private static final Logger LOGGER = Logger.getLogger(ModelLoadSupport.class.getName());

    private final Lock mainLock;
    private final List<ModelLoadListener> listeners;

    public ModelLoadSupport() {
        this.mainLock = new ReentrantLock();
        this.listeners = new LinkedList<>();
    }

    public void addListener(ModelLoadListener listener) {
        if (listener == null) throw new NullPointerException("listener");

        mainLock.lock();
        try {
            // We add the listener to the beginning of the list
            // because we expect that the listener added last will be removed
            // first and if our expectation is met, removing the listener is
            // a constant time operation.
            listeners.add(0, listener);
        } finally {
            mainLock.unlock();
        }
    }

    public void removeListener(ModelLoadListener listener) {
        if (listener == null) throw new NullPointerException("listener");

        mainLock.lock();
        try {
            listeners.remove(listener);
        } finally {
            mainLock.unlock();
        }
    }

    public void fireEvent(NbGradleModel model) {
        if (model == null) throw new NullPointerException("model");

        ModelLoadListener[] currentListeners;

        mainLock.lock();
        try {
            currentListeners = listeners.toArray(new ModelLoadListener[listeners.size()]);
        } finally {
            mainLock.unlock();
        }

        Throwable toThrow = null;
        for (ModelLoadListener listener: currentListeners) {
            try {
                listener.modelLoaded(model);
            } catch (Throwable ex) {
                if (toThrow != null) {
                    LOGGER.log(Level.SEVERE, "Listener has thrown an unexpected exception", ex);
                }
                else {
                    toThrow = ex;
                }
            }
        }

        if (toThrow != null) {
            if (toThrow instanceof RuntimeException) {
                throw (RuntimeException)toThrow;
            }
            if (toThrow instanceof Error) {
                throw (Error)toThrow;
            }

            throw new RuntimeException("Undeclared checked exception has been caught.", toThrow);
        }
    }
}
