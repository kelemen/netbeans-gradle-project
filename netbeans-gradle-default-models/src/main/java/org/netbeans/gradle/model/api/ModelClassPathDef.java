package org.netbeans.gradle.model.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
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

    private static File safeCanonFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file;
        }
    }

    private static Set<File> safeCanonFiles(Collection<? extends File> files) {
        Set<File> result = new LinkedHashSet<File>(4 * files.size() / 3 + 1);
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
     */
    public static ModelClassPathDef fromJarFiles(ClassLoader classLoader, Collection<? extends File> jarFiles) {
        return new ModelClassPathDef(classLoader, jarFiles);
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
