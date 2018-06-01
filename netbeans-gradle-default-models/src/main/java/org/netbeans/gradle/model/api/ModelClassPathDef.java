package org.netbeans.gradle.model.api;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.BuildAction;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the class path required by a particular {@link GradleInfoQuery}.
 *
 * @see GradleProjectInfoQuery
 */
public final class ModelClassPathDef {
    /**
     * A {@code ModelClassPathDef} instance containing no classpath entry and
     * the class loader used to load {@code ModelClassPathDef}.
     */
    public static final ModelClassPathDef EMPTY = new ModelClassPathDef(ModelClassPathDef.class.getClassLoader());
    private static final Map<String, URL> EXCLUDED_PATHS = excludedPaths();

    private final ClassLoader classLoader;
    private final Map<String, URL> jarUrls;

    private ModelClassPathDef(ClassLoader classLoader) {
        if (classLoader == null) throw new NullPointerException("classLoader");

        this.classLoader = classLoader;
        this.jarUrls = Collections.emptyMap();
    }

    private ModelClassPathDef(ClassLoader classLoader, Collection<? extends URL> jarUrls) {
        if (classLoader == null) throw new NullPointerException("classLoader");

        this.classLoader = classLoader;
        this.jarUrls = Collections.unmodifiableMap(toUrlMap(jarUrls));
    }

    private static Map<String, URL> toUrlMap(Collection<? extends URL> urls) {
        Map<String, URL> result = new HashMap<String, URL>(2 * urls.size());
        addToUrlMap(urls, result);
        return result;
    }

    private static void addToUrlMap(URL url, Map<String, URL> result) {
        addToUrlMap(Collections.singletonList(url), result);
    }

    private static void addToUrlMap(Collection<? extends URL> urls, Map<String, URL> result) {
        for (URL url: urls) {
            result.put(url.toExternalForm(), url);
        }
    }

    private static Map<String, URL> excludedPaths() {
        Map<String, URL> result = new HashMap<String, URL>();
        addToUrlMap(ClassLoaderUtils.getUrlOfClassPath(), result);
        addToUrlMap(ClassLoaderUtils.findUrlClassPathOfClass(BuildAction.class), result);
        return Collections.unmodifiableMap(result);
    }

    /**
     * @deprecated Use {@link #isImplicitlyAssumed(URL) isImplicitlyAssumed(URL)} instead.
     *
     * Determines if the given classpath is implicitly assumed and therefore
     * cannot be part of a {@code ModelClassPathDef} instance.
     * <P>
     * This method assumes that the given classpath is in a canonical form
     * (see {@link File#getCanonicalFile()}).
     *
     * @param classPath the classpath entry to be checked if it is implicitly
     *   assumed. This argument cannot be {@code null}.
     * @return {@code true} if the given classpath entry is implicitly assumed,
     *   and must not be explicitly specified by {@code ModelClassPathDef},
     *   {@code false} otherwise
     */
    @Deprecated
    public static boolean isImplicitlyAssumed(File classPath) {
        if (classPath == null) throw new NullPointerException("classPath");
        for (URL url: EXCLUDED_PATHS.values()) {
            if (classPath.equals(ClassLoaderUtils.extractPathFromURL(url))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given classpath is implicitly assumed and therefore
     * cannot be part of a {@code ModelClassPathDef} instance.
     * <P>
     * This method assumes that the given classpath is in a canonical form
     * (see {@link File#getCanonicalFile()}).
     *
     * @param classPath the classpath entry to be checked if it is implicitly
     *   assumed. This argument cannot be {@code null}.
     * @return {@code true} if the given classpath entry is implicitly assumed,
     *   and must not be explicitly specified by {@code ModelClassPathDef},
     *   {@code false} otherwise
     */
    public static boolean isImplicitlyAssumed(URL classPath) {
        if (classPath == null) throw new NullPointerException("classPath");
        return EXCLUDED_PATHS.containsKey(classPath.toExternalForm());
    }

    private static File safeCanonFile(File file) {
        File canonFile;

        try {
            canonFile = file.getCanonicalFile();
        } catch (IOException ex) {
            canonFile = file;
        }

        if (isImplicitlyAssumed(canonFile)) {
            throw new IllegalArgumentException("The given classpath is assumed implicitly and cannot be added: " + file);
        }

        return canonFile;
    }

    private static Set<File> safeCanonFiles(Collection<? extends File> files) {
        Set<File> result = CollectionUtils.newLinkedHashSet(files.size());
        for (File file: files) {
            result.add(safeCanonFile(file));
        }
        return result;
    }

    /**
     * @deprecated Use {@link #fromJarUrls(ClassLoader, Collection) fromJarUrls} instead.
     *
     * Creates a classpath from the given set of jar files (or other classpath
     * entries).
     *
     * @param classLoader the {@code ClassLoader} which is to be used to
     *   deserialize models received from the associated {@link ProjectInfoBuilder}.
     *   This argument cannot be {@code null}.
     * @param jarFiles the jar files to be needed to serialize / deserialize
     *   the associated {@code GradleInfoQuery}. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the classpath from the given set of jar files. This method
     *   never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified classpath contains
     *   an entry which is implicitly assumed and therefore cannot be added
     *   for performance reasons
     *
     * @see #isImplicitlyAssumed(File)
     */
    @Deprecated
    public static ModelClassPathDef fromJarFiles(ClassLoader classLoader, Collection<? extends File> jarFiles) {
        List<URL> urls = new ArrayList<URL>();
        for (File file: jarFiles) {
            try {
                urls.add(file.toURL());
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
        return new ModelClassPathDef(classLoader, urls);
    }

    /**
     * Creates a classpath from the given set of jar files (or other classpath
     * entries).
     *
     * @param classLoader the {@code ClassLoader} which is to be used to
     *   deserialize models received from the associated {@link ProjectInfoBuilder}.
     *   This argument cannot be {@code null}.
     * @param jarFiles the jar files to be needed to serialize / deserialize
     *   the associated {@code GradleInfoQuery}. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the classpath from the given set of jar files. This method
     *   never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified classpath contains
     *   an entry which is implicitly assumed and therefore cannot be added
     *   for performance reasons
     *
     * @see #isImplicitlyAssumed(File)
     */
    public static ModelClassPathDef fromJarUrls(ClassLoader classLoader, Collection<? extends URL> jarFiles) {
        return new ModelClassPathDef(classLoader, jarFiles);
    }

    /**
     * Creates a classpath from the given classes, so that those classes will be
     * on the returned classpath.
     *
     * @param classLoader the {@code ClassLoader} which is to be used to
     *   deserialize models received from the associated {@link ProjectInfoBuilder}.
     *   This argument cannot be {@code null}.
     * @param classes the classes from where the classpath is deduced for
     *   the associated {@code GradleInfoQuery}. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the classpath from the given set of types. This method
     *   never returns {@code null}.
     *
     * @see #isImplicitlyAssumed(File)
     */
    public static ModelClassPathDef fromClasses(ClassLoader classLoader, Collection<? extends Class<?>> classes) {
        Set<File> modelClassPath = new LinkedHashSet<File>();
        for (Class<?> type: classes) {
            File classpath = getClassPathOfClass(type);
            if (!isImplicitlyAssumed(classpath)) {
                modelClassPath.add(classpath);

            }
        }

        if (modelClassPath.isEmpty()) {
            return ModelClassPathDef.EMPTY;
        }

        return ModelClassPathDef.fromJarFiles(classLoader, modelClassPath);
    }

    /**
     * @deprecated Use {@link #getUrlClassPathOfClass(Class) getUrlClassPathOfClass} instead of this method.
     *
     * Returns the classpath from which the given type were loaded. This is
     * usually the jar file containing the given class.
     *
     * @param type the class whose classpath is to be returned. This argument
     *   cannot be {@code null}.
     * @return the classpath from which the given type were loaded. This method
     *   never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the given class was not loaded
     *   from the default file system
     */
    @Deprecated
    public static File getClassPathOfClass(Class<?> type) {
        return ClassLoaderUtils.findClassPathOfClass(type);
    }

    /**
     * Returns the classpath from which the given type were loaded. This is
     * usually the jar file containing the given class.
     *
     * @param type the class whose classpath is to be returned. This argument
     *   cannot be {@code null}.
     * @return the classpath from which the given type were loaded. This method
     *   never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the given class was not loaded
     *   from the default file system
     */
    public static URL getUrlClassPathOfClass(Class<?> type) {
        return ClassLoaderUtils.findUrlClassPathOfClass(type);
    }

    /**
     * Returns the {@code ClassLoader} used to deserialize models returned by
     * the associated {@link ProjectInfoBuilder}.
     *
     * @return the {@code ClassLoader} used to deserialize models returned by
     *   the associated {@link ProjectInfoBuilder}. This method never returns
     *   {@code null}.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @deprecated Use {@link #getJarUrls() getJarUrls} instead.
     *
     * Returns the set of jar files (or other classpath entries) defining the
     * classpath.
     *
     * @return the set of jar files (or other classpath entries) defining the
     *   classpath. This method never returns {@code null} and the returned
     *   set does not contain {@code null} elements.
     */
    @Deprecated
    public Set<File> getJarFiles() {
        Set<File> jarFiles = new HashSet<File>();
        for (URL url: jarUrls.values()) {
            jarFiles.add(ClassLoaderUtils.extractPathFromURL(url));
        }
        return jarFiles;
    }

    public Collection<URL> getJarUrls() {
        return jarUrls.values();
    }
}
