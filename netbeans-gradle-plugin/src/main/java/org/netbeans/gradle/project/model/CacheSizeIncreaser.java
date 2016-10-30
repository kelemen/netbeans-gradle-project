package org.netbeans.gradle.project.model;

public interface CacheSizeIncreaser {
    public void requiresCacheSize(GradleModelCache cache, int minimumCacheSize);
}
