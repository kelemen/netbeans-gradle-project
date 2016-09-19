package org.netbeans.gradle.model.api;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private static final Set<File> EXCLUDED_PATHS = excludedPaths();

    private final ClassLoader classLoader;
    private final Set<File> jarFiles;

    private ModelClassPathDef(ClassLoader classLoader) {
        if (classLoader == null) throw new NullPointerException("classLoader");

        this.classLoader = classLoader;
        this.jarFiles = Collections.emptySet();
    }

    private ModelClassPathDef(ClassLoader classLoader, Collection<? extends File> jarFiles) {
        if (classLoader == null) throw new NullPointerException("classLoader");

        this.classLoader = classLoader;
        this.jarFiles = safeCanonFiles(jarFiles);

        CollectionUtils.checkNoNullElements(this.jarFiles, "jarFiles");
    }

    private static Set<File> excludedPaths() {
        return Collections.unmodifiableSet(new HashSet<File>(Arrays.<File>asList(
            ClassLoaderUtils.getLocationOfClassPath(),
            ClassLoaderUtils.findClassPathOfClass(BuildAction.class)
        )));
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
    public static boolean isImplicitlyAssumed(File classPath) {
        if (classPath == null) throw new NullPointerException("classPath");
        return EXCLUDED_PATHS.contains(classPath);
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
    public static ModelClassPathDef fromJarFiles(ClassLoader classLoader, Collection<? extends File> jarFiles) {
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
    public static File getClassPathOfClass(Class<?> type) {
        return ClassLoaderUtils.findClassPathOfClass(type);
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
     * Returns the set of jar files (or other classpath entries) defining the
     * classpath.
     *
     * @return the set of jar files (or other classpath entries) defining the
     *   classpath. This method never returns {@code null} and the returned
     *   set does not contain {@code null} elements.
     */
    public Set<File> getJarFiles() {
        return jarFiles;
    }
}
