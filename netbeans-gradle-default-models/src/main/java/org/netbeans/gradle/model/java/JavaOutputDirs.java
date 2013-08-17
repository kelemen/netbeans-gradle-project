package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the build output directories of Java projects of Gradle. This class
 * represents the {@code org.gradle.api.tasks.SourceSetOutput} class of Gradle.
 * Therefore, instances of this class are associated with a particular source
 * set.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaOutputDirs implements Serializable {
    private static final long serialVersionUID = 1L;

    private final File classesDir;
    private final File resourcesDir;
    private final Set<File> otherDirs;

    /**
     * Creates a new {@code JavaOutputDirs} with the given properties.
     *
     * @param classesDir the directory where the compile binaries (class files)
     *   of the associated source set are stored after compilation. This
     *   argument cannot be {@code null}.
     * @param resourcesDir the directory where the resources of the associated
     *   source sets are copied to after the {@code processResources} task of
     *   Gradle. This argument cannot be {@code null}.
     * @param otherDirs additional build output directories of the associated
     *   source set. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public JavaOutputDirs(File classesDir, File resourcesDir, Set<File> otherDirs) {
        if (classesDir == null) throw new NullPointerException("classesDir");
        if (resourcesDir == null) throw new NullPointerException("resourcesDir");
        if (otherDirs == null) throw new NullPointerException("otherDirs");

        this.classesDir = classesDir;
        this.resourcesDir = resourcesDir;
        this.otherDirs = Collections.unmodifiableSet(new HashSet<File>(otherDirs));

        CollectionUtils.checkNoNullElements(this.otherDirs, "otherDirs");
    }

    /**
     * Returns the directory where the compile binaries (class files) of the
     * associated source set are stored after compilation. Note that a source
     * set may contain multiple source roots.
     *
     * @return the directory where the compile binaries (class files) of the
     *   associated source set are stored after compilation. This method
     *   never returns {@code null}.
     */
    public File getClassesDir() {
        return classesDir;
    }

    /**
     * Returns the directory where the resources of the associated source sets
     * are copied to after the {@code processResources} task of Gradle.
     *
     * @return the directory where the resources of the associated source sets
     *   are copied to. This method never returns {@code null}.
     */
    public File getResourcesDir() {
        return resourcesDir;
    }

    /**
     * Returns additional build output directories of the associated source set.
     * That is, directories added by the {@code org.gradle.api.tasks.SourceSetOutput.dir}
     * method.
     *
     * @return additional build output directories of the associated source set.
     *   This method never returns {@code null} and does not contain
     *   {@code null} elements.
     */
    public Set<File> getOtherDirs() {
        return otherDirs;
    }
}
