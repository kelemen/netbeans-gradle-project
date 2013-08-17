package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the class paths for a source set of a Gradle project. That is, the
 * class path required to compile the source set and the class path required
 * at runtime.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaClassPaths implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<File> compileClasspaths;
    private final Set<File> runtimeClasspaths;

    /**
     * Creates a new {@code JavaClassPaths} with the given properties.
     *
     * @param compileClasspaths the class path required to compile the
     *   associated source set. This argument cannot be {@code null} and cannot
     *   contain {@code null} elements.
     * @param runtimeClasspaths the class path required at runtime by the
     *   associated source set. This class path must include artifacts from the
     *   compile class path as well if those artifacts are needed at runtime.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public JavaClassPaths(
            Collection<? extends File> compileClasspaths,
            Collection<? extends File> runtimeClasspaths) {

        if (compileClasspaths == null) throw new NullPointerException("compileClasspaths");
        if (runtimeClasspaths == null) throw new NullPointerException("runtimeClasspaths");

        this.compileClasspaths = Collections.unmodifiableSet(new LinkedHashSet<File>(compileClasspaths));
        this.runtimeClasspaths = Collections.unmodifiableSet(new LinkedHashSet<File>(runtimeClasspaths));

        CollectionUtils.checkNoNullElements(this.compileClasspaths, "compileClasspaths");
        CollectionUtils.checkNoNullElements(this.runtimeClasspaths, "runtimeClasspaths");
    }

    /**
     * Returns the class path required to compile the associated source set.
     *
     * @return the class path required to compile the associated source set.
     *   This method never returns {@code null} and the returned set does not
     *   contain {@code null} elements.
     */
    public Set<File> getCompileClasspaths() {
        return compileClasspaths;
    }

    /**
     * Returns the class path required at runtime by the associated source set.
     * This usually is a super set of the
     * {@link #getCompileClasspaths() compile class path}. Although, this is
     * not necessary. For example, annotation processors are not required at
     * runtime.
     *
     * @return the class path required at runtime by the associated source set.
     *   This method never returns {@code null} and the returned set does not
     *   contain {@code null} elements.
     */
    public Set<File> getRuntimeClasspaths() {
        return runtimeClasspaths;
    }
}
