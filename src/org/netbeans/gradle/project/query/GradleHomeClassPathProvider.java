package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.GradleHomeRegistry;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = ClassPathProvider.class)})
public final class GradleHomeClassPathProvider implements ClassPathProvider {
    private static final URL[] NO_URLS = new URL[0];

    private final SimpleCache<FileObject, ClassPath> sourcePathsCache;
    private final SimpleCache<FileObject, ClassPath> binPathsCache;

    public GradleHomeClassPathProvider() {
        this.sourcePathsCache = new SimpleCache<FileObject, ClassPath>(1);
        this.binPathsCache = new SimpleCache<FileObject, ClassPath>(1);
    }

    public static URL[] getGradleLibs(FileObject gradleHomeObj, FilenameFilter filter) {
        File gradleHome = FileUtil.toFile(gradleHomeObj);
        if (gradleHome == null) {
            return NO_URLS;
        }

        if (!gradleHome.isDirectory()) {
            return NO_URLS;
        }

        File libDir = GradleFileUtils.getLibDirOfGradle(gradleHome);
        if (!libDir.isDirectory()) {
            return NO_URLS;
        }

        File[] jars = libDir.listFiles(filter);
        List<URL> result = new ArrayList<URL>(jars.length);
        for (File jar: jars) {
            URL url = FileUtil.urlForArchiveOrDir(jar);
            if (url != null) {
                result.add(url);
            }
        }

        return result.toArray(NO_URLS);
    }

    public static URL[] getAllGradleLibs(FileObject gradleHomeObj) {
        return getGradleLibs(gradleHomeObj, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.US).endsWith(".jar");
            }
        });
    }

    public static URL[] getGradleBinaries(FileObject gradleHomeObj) {
        return getGradleLibs(gradleHomeObj, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase(Locale.US);
                return lowerCaseName.startsWith("gradle-") && lowerCaseName.endsWith(".jar");
            }
        });
    }

    private ClassPath getSourcePaths(FileObject gradleHome, FileObject file) {
        FileObject srcDir = GradleFileUtils.getSrcDirOfGradle(gradleHome);
        if (srcDir == null || FileUtil.getRelativePath(srcDir, file) == null) {
            return null;
        }

        if (srcDir != null && srcDir.isFolder()) {
            ClassPath classpath = sourcePathsCache.tryGetFromCache(gradleHome);
            if (classpath != null) {
                return classpath;
            }

            classpath = ClassPathSupport.createClassPath(srcDir);
            sourcePathsCache.addToCache(gradleHome, classpath);

            return classpath;
        }
        else {
            return null;
        }
    }

    private ClassPath getBinaryPaths(FileObject gradleHome) {
        ClassPath classpath = binPathsCache.tryGetFromCache(gradleHome);
        if (classpath != null) {
            return classpath;
        }

        URL[] libs = getAllGradleLibs(gradleHome);
        classpath = ClassPathSupport.createClassPath(libs);
        binPathsCache.addToCache(gradleHome, classpath);
        return classpath;
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        FileObject gradleHome = GlobalGradleSettings.getGradleInstallation();
        if (gradleHome == null) {
            return null;
        }

        if (!FileUtil.isParentOf(gradleHome, file)) {
            return null;
        }

        GradleHomeRegistry.requireGradlePaths();

        if (ClassPath.SOURCE.equals(type)) {
            return getSourcePaths(gradleHome, file);
        }
        else if (ClassPath.BOOT.equals(type)) {
            JavaPlatform platform = JavaPlatform.getDefault();
            return platform != null ? platform.getBootstrapLibraries() : null;
        }
        else if (ClassPath.COMPILE.equals(type)) {
            return getBinaryPaths(gradleHome);
        }
        else if (ClassPath.EXECUTE.equals(type)) {
            return getBinaryPaths(gradleHome);
        }
        else if (JavaClassPathConstants.PROCESSOR_PATH.equals(type)) {
            return getBinaryPaths(gradleHome);
        }
        else {
            return null;
        }
    }

    private static class SimpleCache<KeyType, ValueType> {
        private final Lock cacheLock;
        private final Map<KeyType, ValueType> cache;
        private final int maxCapacity;

        public SimpleCache(int maxCapacity) {
            this.cacheLock = new ReentrantLock();

            float loadFactor = 0.75f;
            int capacity = (int)Math.floor((float)(maxCapacity + 1) / loadFactor);
            this.cache = new LinkedHashMap<KeyType, ValueType>(capacity, loadFactor, true);
            this.maxCapacity = maxCapacity;
        }

        public void addToCache(KeyType key, ValueType value) {
            cacheLock.lock();
            try {
                cache.put(key, value);
                while (cache.size() > maxCapacity) {
                    Iterator<?> itr = cache.entrySet().iterator();
                    itr.next();
                    itr.remove();
                }
            } finally {
                cacheLock.unlock();
            }
        }

        public ValueType tryGetFromCache(KeyType key) {
            cacheLock.lock();
            try {
                return cache.get(key);
            } finally {
                cacheLock.unlock();
            }
        }
    }
}
