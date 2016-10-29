package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public final class LazyPersistentModelStoreFactory<T> {
    private static final Logger LOGGER = Logger.getLogger(LazyPersistentModelStoreFactory.class.getName());

    private final ModelPersister<? super T> modelPersister;

    private final UpdateTaskExecutor persisterExecutor;

    private final ReentrantLock queueLock;
    private final Queue<Path> taskQueue;
    private final Map<Path, T> toSave;

    public LazyPersistentModelStoreFactory(ModelPersister<? super T> modelPersister, TaskExecutor persisterExecutor) {
        ExceptionHelper.checkNotNullArgument(modelPersister, "modelPersister");
        this.modelPersister = modelPersister;
        this.persisterExecutor = new GenericUpdateTaskExecutor(persisterExecutor);

        this.queueLock = new ReentrantLock();
        this.taskQueue = new LinkedList<>();
        this.toSave = new HashMap<>();
    }

    public PersistentModelStore<T> createStore(PersistentModelRetriever<? extends T> modelRetriever) {
        return new LazyPersistentModelStore(modelRetriever);
    }

    private final class LazyPersistentModelStore implements PersistentModelStore<T> {
        private final PersistentModelRetriever<? extends T> modelRetriever;

        public LazyPersistentModelStore(PersistentModelRetriever<? extends T> modelRetriever) {
            ExceptionHelper.checkNotNullArgument(modelRetriever, "modelRetriever");
            this.modelRetriever = modelRetriever;
        }

        @Override
        public void persistModel(T model, Path dest) throws IOException {
            ExceptionHelper.checkNotNullArgument(model, "model");
            ExceptionHelper.checkNotNullArgument(dest, "dest");

            queueLock.lock();
            try {
                if (toSave.put(dest, model) == null) {
                    taskQueue.add(dest);
                }
            } finally {
                queueLock.unlock();
            }

            persisterExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    persistQueue();
                }
            });
        }

        private void fixEmptyQueue() {
            assert queueLock.isHeldByCurrentThread();
            assert taskQueue.isEmpty() : "This method may only be called on empty taskQueue";
            // This method is only to recover if there are some inconsistencies
            // between `toSave` and `taskQueue`. Inconsistencies should never happen
            // except in case of a bug.

            if (!toSave.isEmpty()) {
                LOGGER.log(Level.WARNING, "Internal error: Task queue is empty while there are models to save.");
                taskQueue.addAll(toSave.keySet());
            }
        }

        private void persistQueue() {
            while (true) {
                Path dest;
                T model;

                queueLock.lock();
                try {
                    dest = taskQueue.poll();
                    if (dest == null) {
                        fixEmptyQueue();
                        return;
                    }
                    model = toSave.get(dest);
                } finally {
                    queueLock.unlock();
                }

                if (model == null) {
                    LOGGER.log(Level.WARNING, "There is no model to save for path: {0}", dest);
                    continue;
                }

                try {
                    modelPersister.persistModel(model, dest);
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Failed to save into the persistent cache.", ex);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected error while saving to the persistent cache.", ex);
                }
            }
        }

        @Override
        public T tryLoadModel(Path src) throws IOException {
            queueLock.lock();
            try {
                T model = toSave.get(src);
                if (model != null) {
                    return model;
                }
            } finally {
                queueLock.unlock();
            }

            return modelRetriever.tryLoadModel(src);
        }
    }
}
