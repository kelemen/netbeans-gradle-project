package org.netbeans.gradle.project.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleModelCache {
    private static final AtomicReference<GradleModelCache> DEFAULT_REF
            = new AtomicReference<GradleModelCache>(null);

    private final ReentrantLock cacheLock;
    private final Map<CacheKey, NbGradleModel> cache;
    private final AtomicInteger maxCapacity;
    private final PropertyChangeSupport updateListeners;

    public GradleModelCache(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + maxCapacity);
        }

        this.cacheLock = new ReentrantLock();
        this.maxCapacity = new AtomicInteger(maxCapacity);

        float loadFactor = 0.75f;
        int capacity = (int)Math.floor((float)(maxCapacity + 1) / loadFactor);
        this.cache = new LinkedHashMap<CacheKey, NbGradleModel>(capacity, loadFactor, true);
        this.updateListeners = new PropertyChangeSupport(this);
    }

    private static int getProjectCacheSize() {
        return GlobalGradleSettings.getProjectCacheSize().getValue();
    }

    public static GradleModelCache getDefault() {
        GradleModelCache result = DEFAULT_REF.get();
        if (result == null) {
            result = new GradleModelCache(getProjectCacheSize());
            if (DEFAULT_REF.compareAndSet(null, result)) {
                final GradleModelCache cache = result;
                GlobalGradleSettings.getProjectCacheSize().addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        cache.setMaxCapacity(getProjectCacheSize());
                    }
                });
                cache.setMaxCapacity(getProjectCacheSize());
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
        if (model == null) throw new NullPointerException("model");

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

    public NbListenerRef addModelUpdateListener(final ProjectModelUpdatedListener listener) {
        if (listener == null) throw new NullPointerException("listener");

        final PropertyChangeListener forwarder = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                listener.onUpdateProject((NbGradleModel)evt.getNewValue());
            }
        };

        updateListeners.addPropertyChangeListener(forwarder);
        return new NbListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                updateListeners.removePropertyChangeListener(forwarder);
                registered = true;
            }
        };
    }

    private void notifyUpdate(NbGradleModel newModel) {
        updateListeners.firePropertyChange("newModel", null, newModel);
    }

    public void updateEntry(NbGradleModel model) {
        CacheKey key = tryCreateKey(model);
        if (key == null) {
            return;
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
            if (projectDir == null) throw new NullPointerException("buildFile");
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
}
