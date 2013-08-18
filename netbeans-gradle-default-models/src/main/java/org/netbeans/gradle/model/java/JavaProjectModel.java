package org.netbeans.gradle.model.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the properties of a Java project of Gradle. For generic properties
 * common to all projects, see the
 * {@link org.netbeans.gradle.model.GenericProjectProperties GenericProjectProperties}
 * class.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 *
 * @see org.netbeans.gradle.model.GenericProjectProperties
 */
public final class JavaProjectModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sourceCompatibility;
    private final String targetCompatibility;
    private final Collection<JavaSourceSet> sourceSets;

    public JavaProjectModel(
            String sourceCompatibility,
            String targetCompatibility,
            Collection<? extends JavaSourceSet> sourceSets) {
        if (sourceCompatibility == null) throw new NullPointerException("sourceCompatibility");
        if (targetCompatibility == null) throw new NullPointerException("targetCompatibility");
        if (sourceSets == null) throw new NullPointerException("sourceSets");

        this.sourceCompatibility = sourceCompatibility;
        this.targetCompatibility = targetCompatibility;
        this.sourceSets = Collections.unmodifiableList(new ArrayList<JavaSourceSet>(sourceSets));

        CollectionUtils.checkNoNullElements(this.sourceSets, "sourceSets");
    }

    /**
     * Returns the Java to use to determine the allowed syntax when compiling
     * the sources. For example, diamond syntax is only allowed from version
     * "1.7". This property is determined by the "sourceCompatibility" of the
     * <A href="http://www.gradle.org/docs/current/userguide/java_plugin.html">Java plugin of Gradle</A>.
     * <P>
     * This method returns the version in the form: "1.X". That is, the same
     * way as {@code org.gradle.api.JavaVersion.toString()} does.
     *
     * @return the Java to use to determine the allowed syntax when compiling
     *   the sources. This method never returns {@code null}.
     */
    public String getSourceCompatibility() {
        return sourceCompatibility;
    }

    /**
     * Returns the Java version to use to generate class for. That is, JVM with
     * a version of at least the returned value should be able to load the
     * class (although, if you have been using classes from a newer JDK, they
     * won't be found). This property is determined by the "targetCompatibility" of the
     * <A href="http://www.gradle.org/docs/current/userguide/java_plugin.html">Java plugin of Gradle</A>.
     * <P>
     * This method returns the version in the form: "1.X". That is, the same
     * way as {@code org.gradle.api.JavaVersion.toString()} does.
     *
     * @return the Java version to use to generate class for. This method never
     *   returns {@code null}.
     */
    public String getTargetCompatibility() {
        return targetCompatibility;
    }

    /**
     * Returns the source sets containing the source files of this Java project.
     *
     * @return the source sets containing the source files of this Java project.
     *   This method never returns {@code null} and the elements of the returned
     *   collection cannot be {@code null}.
     */
    public Collection<JavaSourceSet> getSourceSets() {
        return sourceSets;
    }
}
