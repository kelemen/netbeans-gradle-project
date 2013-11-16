package org.netbeans.gradle.model.api;

import java.io.File;
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
     * A {@code ModelClassPathDef} instance containing no classpath entry.
     */
    public static final ModelClassPathDef EMPTY = new ModelClassPathDef();

    private final Set<File> jarFiles;

    private ModelClassPathDef() {
        this.jarFiles = Collections.emptySet();
    }

    private ModelClassPathDef(Collection<? extends File> jarFiles) {
        this.jarFiles = new LinkedHashSet<File>(jarFiles);

        CollectionUtils.checkNoNullElements(this.jarFiles, "jarFiles");
    }

    /**
     * Creates a classpath from the given set of jar files (or other classpath
     * entries).
     *
     * @param jarFiles the jar files to be needed to serialize / deserialize
     *   the associated {@code GradleInfoQuery}. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the classpath from the given set of jar files. This method
     *   never returns {@code null}.
     */
    public static ModelClassPathDef fromJarFiles(Collection<? extends File> jarFiles) {
        return new ModelClassPathDef(jarFiles);
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
