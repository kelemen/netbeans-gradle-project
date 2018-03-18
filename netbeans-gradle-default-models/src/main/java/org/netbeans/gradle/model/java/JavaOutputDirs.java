package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    private final Set<File> classesDirs;
    private final File resourcesDir;
    private final Set<File> otherDirs;

    /**
     * Creates a new {@code JavaOutputDirs} with the given properties.
     *
     * @param classesDirs the directories where the compile binaries (class files)
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
    public JavaOutputDirs(Collection<File> classesDirs, File resourcesDir, Collection<? extends File> otherDirs) {
        if (classesDirs == null) throw new NullPointerException("classesDir");
        if (resourcesDir == null) throw new NullPointerException("resourcesDir");
        if (otherDirs == null) throw new NullPointerException("otherDirs");

        this.classesDirs = Collections.unmodifiableSet(new HashSet<File>(classesDirs));
        this.resourcesDir = resourcesDir;
        this.otherDirs = Collections.unmodifiableSet(new LinkedHashSet<File>(otherDirs));

        CollectionUtils.checkNoNullElements(this.classesDirs, "classesDirs");
        CollectionUtils.checkNoNullElements(this.otherDirs, "otherDirs");
    }

    /**
     * Returns the directories where the compile binaries (class files) of the
     * associated source set are stored after compilation. Note that a source
     * set may contain multiple source roots.
     *
     * @return the directory where the compile binaries (class files) of the
     *   associated source set are stored after compilation. This method
     *   never returns {@code null}.
     */
    public Set<File> getClassesDirs() {
        return classesDirs;
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

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Set<File> classesDirs;
        private final File resourcesDir;
        private final Set<File> otherDirs;

        public SerializedFormat(JavaOutputDirs source) {
            this.classesDirs = source.classesDirs;
            this.resourcesDir = source.resourcesDir;
            this.otherDirs = source.otherDirs;
        }

        private Object readResolve() throws ObjectStreamException {
            return new JavaOutputDirs(classesDirs, resourcesDir, otherDirs);
        }
    }
}
