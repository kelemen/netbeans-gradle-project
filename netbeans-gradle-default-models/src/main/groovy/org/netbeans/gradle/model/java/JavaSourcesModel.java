package org.netbeans.gradle.model.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the source directories (and their dependencies) of a Java or other
 * JVM based projects. This model is applicable for all Gradle projects applying
 * the "java" plugin (explicitly or implicitly). Therefore, this model can be
 * retrieved for Scala and Groovy projects as well.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaSourcesModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Collection<JavaSourceSet> sourceSets;

    /**
     * Creates a new {@code JavaSourcesModel} with the given source sets.
     *
     * @param sourceSets the source sets of the Java project. This argument
     *   cannot be {@code null} and none of its elements can be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or one of its element
     *   is {@code null}
     */
    public JavaSourcesModel(Collection<? extends JavaSourceSet> sourceSets) {
        this.sourceSets = Collections.unmodifiableList(new ArrayList<JavaSourceSet>(sourceSets));
        CollectionUtils.checkNoNullElements(this.sourceSets, "sourceSets");
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
