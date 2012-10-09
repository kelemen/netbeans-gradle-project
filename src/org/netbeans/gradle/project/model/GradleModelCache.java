package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleModelCache {
    private final Lock cacheLock;
    private final Map<CacheKey, NbGradleModel> cache;
    private final int maxCapacity;

    public GradleModelCache(int maxCapacity) {
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("Illegal max. capacity value: " + maxCapacity);
        }

        this.cacheLock = new ReentrantLock();
        this.maxCapacity = maxCapacity;

        float loadFactor = 0.75f;
        int capacity = (int)Math.floor((float)(maxCapacity + 1) / loadFactor);
        this.cache = new LinkedHashMap<CacheKey, NbGradleModel>(capacity, loadFactor, true);
    }

    public void addToCache(NbGradleModel model) {
        if (model == null) throw new NullPointerException("model");

        File projectDir = FileUtil.toFile(model.getProjectDir());

        FileObject settingsFileObj = model.getSettingsFile();
        File settingsFile = settingsFileObj != null
                ? FileUtil.toFile(settingsFileObj)
                : null;

        if (projectDir == null || (settingsFile == null && settingsFileObj != null)) {
            return;
        }

        CacheKey key = new CacheKey(projectDir, settingsFile);

        cacheLock.lock();
        try {
            cache.put(key, model);

            // The body of the while loop should not be executed more than once
            // but keep the while (instead of "if"), in case it is required due
            // to some bugs.
            while (cache.size() > maxCapacity) {
                Iterator<?> itr = cache.entrySet().iterator();
                itr.next();
                itr.remove();
            }
        } finally {
            cacheLock.unlock();
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
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CacheKey other = (CacheKey)obj;
            if (this.projectDir != other.projectDir && (!this.projectDir.equals(other.projectDir))) {
                return false;
            }
            if (this.settingsFile != other.settingsFile && (this.settingsFile == null || !this.settingsFile.equals(other.settingsFile))) {
                return false;
            }
            return true;
        }
    }
}
