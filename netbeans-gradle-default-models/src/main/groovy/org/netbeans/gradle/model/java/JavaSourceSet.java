package org.netbeans.gradle.model.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import org.netbeans.gradle.model.GenericSourceGroup;

/**
 * Defines the source set of a Gradle Java project. A source set is set of
 * source files used in a single compilation step. For example, in Java projects
 * you have a "main" source set by default which can be accessed in the build
 * script as shown in the code below:
 * <pre>
 * sourceSets {
 *     main {
 *         // ...
 *     }
 * }
 * </pre>
 * <P>
 * This class can be instantiated through a its {@link Builder builder} as in
 * the code below:
 * <pre>
 * JavaSourceSet.Builder builder = new JavaSourceSet.Builder(name, outputDirs);
 * builder.setClasspaths(classpaths);
 * builder.addSourceGroup(sourceGroup1);
 * builder.addSourceGroup(sourceGroup2);
 * JavaSourceSet sourceSet = builder.create();
 * </pre>
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaSourceSet implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A builder to create new instances of {@link JavaSourceSet}.
     * Once you have initialized all the properties, you have to call
     * {@link #create() create()} to create a new {@code JavaSourceSet} instance.
     * <P>
     * Note that instances of this builder must not be accessed concurrently by
     * multiple threads. Although it is possible to synchronize access to
     * method calls to this builder, it is not recommended. The recommended
     * usage of this class, is to create a builder initialize its fields,
     * create the {@code JavaSourceSet} and discard the reference to the
     * builder.
     */
    public static final class Builder {
        private final String name;
        private final JavaOutputDirs outputDirs;
        private final Collection<GenericSourceGroup> sourceGroups;
        private JavaClassPaths classpaths;

        /**
         * Creates a builder initialized with the specified properties and some
         * default values.
         * <P>
         * The default values for properties:
         * <ul>
         * <li>sourceGroups: No source groups (empty collection).</li>
         * <li>classpaths: Empty class paths.</li>
         * </ul>
         *
         * @param name the name of this source set as defined in the build
         *   scripts. This argument cannot be {@code null}.
         * @param outputDirs the build output directories defined for this
         *   source set. This argument cannot be {@code null}.
         *
         * @throws NullPointerException thrown if any of the arguments is
         *   {@code null}
         */
        public Builder(String name, JavaOutputDirs outputDirs) {
            if (name == null) throw new NullPointerException("name");
            if (outputDirs == null) throw new NullPointerException("outputDirs");

            this.name = name;
            this.outputDirs = outputDirs;
            this.sourceGroups = new LinkedList<GenericSourceGroup>();
            this.classpaths = JavaClassPaths.EMPTY;
        }

        /**
         * Adds an additional source group to the source set. By default, there
         * is no source set added to the this builder.
         *
         * @param sourceGroup the source group to add to the source set. This
         *   argument cannot be {@code null}.
         *
         * @throws NullPointerException thrown if the specified source group
         *   is {@code null}
         */
        public void addSourceGroup(GenericSourceGroup sourceGroup) {
            if (sourceGroup == null) throw new NullPointerException("sourceGroup");
            sourceGroups.add(sourceGroup);
        }

        /**
         * Sets the class paths for the source set. Multiple invocations of
         * this method overwrite each other's work (so only the last call has
         * effect).
         *
         * @param classpaths the class paths for the source set. This argument
         *   cannot be {@code null}.
         *
         * @throws NullPointerException thrown if the specified class path is
         *   {@code null}
         */
        public void setClasspaths(JavaClassPaths classpaths) {
            if (classpaths == null) throw new NullPointerException("classpaths");
            this.classpaths = classpaths;
        }

        /**
         * Creates a new instance of {@code JavaSourceSet} initialized with the
         * properties currently set for this builder. Adjusting the properties
         * of this builder has no effect on the {@code JavaSourceSet} created.
         *
         * @return a new instance of {@code JavaSourceSet} initialized with the
         *   properties currently set for this builder. This method never
         *   returns {@code null}.
         */
        public JavaSourceSet create() {
            return new JavaSourceSet(this);
        }
    }

    private final String name;
    private final JavaOutputDirs outputDirs;
    private final Collection<GenericSourceGroup> sourceGroups;
    private final JavaClassPaths classpaths;

    private JavaSourceSet(Builder builder) {
        this.name = builder.name;
        this.outputDirs = builder.outputDirs;
        this.sourceGroups = new ArrayList<GenericSourceGroup>(builder.sourceGroups);
        this.classpaths = builder.classpaths;
    }

    /**
     * Returns the name of this source set as defined in the build scripts.
     * For example:
     * <pre>
     * sourceSets {
     *     main {
     *         // ...
     *     }
     * }
     * </pre>
     * In the code above, the name of the source set is "main".
     *
     * @return the name of this source set as defined in the build scripts. This
     *   method never returns {@code null}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the build output directories defined for this source set. For
     * example, a build output directory is "build/classes" (by default).
     *
     * @return the build output directories defined for this source set. This
     *   method never returns {@code null}.
     */
    public JavaOutputDirs getOutputDirs() {
        return outputDirs;
    }

    /**
     * Returns the source groups defining the source files of this source set.
     *
     * @return the source groups defining the source files of this source set.
     *   This method never returns {@code null} and the returned collection
     *   does not contain {@code null} elements.
     */
    public Collection<GenericSourceGroup> getSourceGroups() {
        return sourceGroups;
    }

    /**
     * Returns the class paths required by this source set to be compiled and be
     * executed.
     *
     * @return the class paths required by this source set. This method never
     *   returns {@code null}.
     */
    public JavaClassPaths getClasspaths() {
        return classpaths;
    }
}
