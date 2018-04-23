package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.GradleHomeRegistry;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.DefaultUrlFactory;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.UrlFactory;
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
        this.sourcePathsCache = new SimpleCache<>(1);
        this.binPathsCache = new SimpleCache<>(1);
    }

    private static File[] tryListFiles(File dir, FilenameFilter filter) {
        if (!dir.isDirectory()) {
            return null;
        }

        return dir.listFiles(filter);
    }

    private static List<File> getLibsFromGradleRoot(File gradleHome, FilenameFilter filter) {
        if (!gradleHome.isDirectory()) {
            return Collections.emptyList();
        }

        File libDir = GradleFileUtils.getLibDirOfGradle(gradleHome);
        File[] jars = tryListFiles(libDir, filter);
        if (jars == null) {
            return Collections.emptyList();
        }

        List<File> result = new ArrayList<>(Arrays.asList(jars));

        File pluginsDir = new File(libDir, "plugins");
        File[] pluginJars = tryListFiles(pluginsDir, filter);
        if (pluginJars != null) {
            result.addAll(Arrays.asList(pluginJars));
        }

        return result;
    }

    public static URL[] getGradleLibs(FileObject gradleHomeObj, FilenameFilter filter) {
        File gradleHome = FileUtil.toFile(gradleHomeObj);
        if (gradleHome == null || !gradleHome.isDirectory()) {
            return NO_URLS;
        }

        List<File> jars = getLibsFromGradleRoot(gradleHome, filter);
        if (jars.isEmpty()) {
            return NO_URLS;
        }

        UrlFactory urlFactory = DefaultUrlFactory.getDefaultArchiveOrDirFactory();

        List<URL> result = new ArrayList<>(jars.size());
        for (File jar: jars) {
            URL url = urlFactory.toUrl(jar);
            if (url != null) {
                result.add(url);
            }
        }

        return result.toArray(NO_URLS);
    }

    public static URL[] getAllGradleLibs(FileObject gradleHomeObj) {
        return getGradleLibs(gradleHomeObj, (File dir, String name) -> {
            return name.toLowerCase(Locale.US).endsWith(".jar");
        });
    }

    public static URL[] getGradleBinaries(FileObject gradleHomeObj) {
        return getGradleLibs(gradleHomeObj, (File dir, String name) -> {
            String lowerCaseName = name.toLowerCase(Locale.US);
            return lowerCaseName.startsWith("gradle-") && lowerCaseName.endsWith(".jar");
        });
    }

    private ClassPath getSourcePaths(FileObject gradleHome, FileObject file) {
        FileObject srcDir = GradleFileUtils.getSrcDirOfGradle(gradleHome);
        if (srcDir == null || FileUtil.getRelativePath(srcDir, file) == null) {
            return null;
        }

        if (srcDir.isFolder()) {
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
        if (type == null) {
            return null;
        }

        FileObject gradleHome = CommonGlobalSettings.getDefault().tryGetGradleInstallation();
        if (gradleHome == null) {
            return null;
        }

        if (!FileUtil.isParentOf(gradleHome, file)) {
            return null;
        }

        GradleHomeRegistry.requireGradlePaths();
        switch (type) {
            case ClassPath.SOURCE:
                return getSourcePaths(gradleHome, file);
            case ClassPath.BOOT:
                JavaPlatform platform = JavaPlatform.getDefault();
                return platform != null ? platform.getBootstrapLibraries() : null;
            case ClassPath.COMPILE:
                return getBinaryPaths(gradleHome);
            case ClassPath.EXECUTE:
                return getBinaryPaths(gradleHome);
            case JavaClassPathConstants.PROCESSOR_PATH:
                return getBinaryPaths(gradleHome);
            default:
                return null;
        }
    }

    private static class SimpleCache<KeyType, ValueType> {
        private final Lock cacheLock;
        private final Map<KeyType, ValueType> cache;
        private final int maxCapacity;

        public SimpleCache(int maxCapacity) {
            this.cacheLock = new ReentrantLock();

            this.cache = CollectionUtils.newLinkedHashMap(maxCapacity);
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
