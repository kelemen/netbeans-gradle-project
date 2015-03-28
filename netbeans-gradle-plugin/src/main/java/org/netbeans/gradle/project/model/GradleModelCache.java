package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleModelCache {
    private static final AtomicReference<GradleModelCache> DEFAULT_REF
            = new AtomicReference<>(null);

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

    public static GradleModelCache getDefault() {
        GradleModelCache result = DEFAULT_REF.get();
        if (result == null) {
            final PropertySource<Integer> cacheSize = GlobalGradleSettings.getDefault().projectCacheSize();
            result = new GradleModelCache(cacheSize.getValue());
            if (DEFAULT_REF.compareAndSet(null, result)) {
                final GradleModelCache cache = result;
                cacheSize.addChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        cache.setMaxCapacity(cacheSize.getValue());
                    }
                });
                cache.setMaxCapacity(cacheSize.getValue());
            }
            else {
                result = DEFAULT_REF.get();
            }
        }
        return result;
    }

    private void cleanupCache() {
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

    public void setMaxCapacity(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + maxCapacity);
        }

        int prevCapacity = this.maxCapacity.getAndSet(maxCapacity);
        if (prevCapacity > maxCapacity) {
            cacheLock.lock();
            try {
                cleanupCache();
            } finally {
                cacheLock.unlock();
            }
        }
    }

    private static CacheKey tryCreateKey(NbGradleModel model) {
        ExceptionHelper.checkNotNullArgument(model, "model");

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
                cleanupCache();
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
            cleanupCache();
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
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
            this.projectDir = projectDir;
            this.settingsFile = settingsFile;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + this.projectDir.hashCode();
            hash = 89 * hash + (this.settingsFile != null ? this.settingsFile.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final CacheKey other = (CacheKey)obj;

            if (this.projectDir != other.projectDir && (!this.projectDir.equals(other.projectDir))) {
                return false;
            }
            return this.settingsFile == other.settingsFile
                    || (this.settingsFile != null && this.settingsFile.equals(other.settingsFile));
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
