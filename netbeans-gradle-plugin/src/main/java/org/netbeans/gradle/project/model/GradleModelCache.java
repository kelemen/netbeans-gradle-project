package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.EventDispatcher;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleModelCache {
    private final ReentrantLock cacheLock;
    private final Map<CacheKey, NbGradleModel> cache;
    private final AtomicInteger maxCapacity;
    private final ListenerManager<ProjectModelUpdatedListener> updateListeners;

    public GradleModelCache(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + maxCapacity);
        }

        this.cacheLock = new ReentrantLock();
        this.maxCapacity = new AtomicInteger(maxCapacity);

        this.cache = CollectionUtils.newLinkedHashMap(maxCapacity);
        this.updateListeners = new CopyOnTriggerListenerManager<>();
    }

    private void cleanupCacheUnsafe() {
        assert cacheLock.isHeldByCurrentThread();

        int currentMaxCapacity = maxCapacity.get();
        // Don't create the iterator.
        if (cache.size() <= currentMaxCapacity) {
            return;
        }

        Iterator<?> itr = cache.entrySet().iterator();
        while (cache.size() > currentMaxCapacity && itr.hasNext()) {
            itr.next();
            itr.remove();
        }
    }

    private void cleanupCache() {
        cacheLock.lock();
        try {
            cleanupCacheUnsafe();
        } finally {
            cacheLock.unlock();
        }
    }

    public int getMaxCapacity() {
        return maxCapacity.get();
    }

    public void setMaxCapacity(int newMaxCapacity) {
        if (newMaxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + newMaxCapacity);
        }

        int prevCapacity = maxCapacity.getAndSet(newMaxCapacity);
        if (prevCapacity > newMaxCapacity) {
            cleanupCache();
        }
    }

    public void setMaxCapacityToAtLeast(int newMaxCapacity) {
        if (newMaxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + newMaxCapacity);
        }

        int prevCapacity = maxCapacity.get();
        do {
            if (prevCapacity >= newMaxCapacity) {
                break;
            }
        } while (maxCapacity.compareAndSet(prevCapacity, newMaxCapacity));
    }

    private static CacheKey tryCreateKey(NbGradleModel model) {
        Objects.requireNonNull(model, "model");

        File projectDir = model.getGenericInfo().getProjectDir();

        FileObject settingsFileObj = model.getGenericInfo().tryGetSettingsFileObj();
        File settingsFile = settingsFileObj != null
                ? FileUtil.toFile(settingsFileObj)
                : null;

        if (projectDir == null || (settingsFile == null && settingsFileObj != null)) {
            return null;
        }

        return new CacheKey(projectDir, settingsFile);
    }

    public ListenerRef addModelUpdateListener(ProjectModelUpdatedListener listener) {
        return updateListeners.registerListener(listener);
    }

    private void notifyUpdate(NbGradleModel newModel) {
        updateListeners.onEvent(ModelUpdateDispatcher.INSTANCE, newModel);
    }

    public NbGradleModel updateEntry(NbGradleModel model) {
        CacheKey key = tryCreateKey(model);
        if (key == null) {
            return null;
        }

        NbGradleModel newModel = model;
        NbGradleModel prevModel;
        cacheLock.lock();
        try {
            prevModel = cache.get(key);
            if (prevModel == null) {
                cache.put(key, newModel);
                cleanupCacheUnsafe();
            }
            else {
                newModel = prevModel.updateEntry(newModel);
                cache.put(key, newModel);
            }
        } finally {
            cacheLock.unlock();
        }

        if (prevModel != null) {
            notifyUpdate(model);
        }
        return newModel;
    }

    public void replaceEntry(NbGradleModel model) {
        CacheKey key = tryCreateKey(model);
        if (key == null) {
            return;
        }

        NbGradleModel prevModel;
        cacheLock.lock();
        try {
            prevModel = cache.put(key, model);
            cleanupCacheUnsafe();
        } finally {
            cacheLock.unlock();
        }

        if (prevModel != null && prevModel != model) {
            notifyUpdate(model);
        }
    }

    public NbGradleModel tryGet(File projectDir, File settingsFile) {
        CacheKey key = new CacheKey(projectDir, settingsFile);
        cacheLock.lock();
        try {
            return cache.get(key);
        } finally {
            cacheLock.unlock();
        }
    }

    private static class CacheKey {
        private final File projectDir;
        private final File settingsFile;

        public CacheKey(File projectDir, File settingsFile) {
            this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
            this.settingsFile = settingsFile;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + projectDir.hashCode();
            hash = 89 * hash + Objects.hashCode(settingsFile);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final CacheKey other = (CacheKey)obj;

            return Objects.equals(this.settingsFile, other.settingsFile)
                    && Objects.equals(this.projectDir, other.projectDir);
        }
    }

    private enum ModelUpdateDispatcher implements EventDispatcher<ProjectModelUpdatedListener, NbGradleModel> {
        INSTANCE;

        @Override
        public void onEvent(ProjectModelUpdatedListener eventListener, NbGradleModel arg) {
            eventListener.onUpdateProject(arg);
        }
    }
}
