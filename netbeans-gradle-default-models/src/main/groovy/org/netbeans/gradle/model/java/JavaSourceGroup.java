package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines a set of source roots of a Gradle project. For example, a source
 * group can be Java source files or resource files within a particular source
 * set.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaSourceGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JavaSourceGroupName groupName;
    private final Set<File> sourceRoots;

    /**
     * Creates a new {@code JavaSourceGroup} with the given properties.
     *
     * @param groupName the name of these source roots representing the type of
     *   sources they contain. This argument cannot be {@code null}.
     * @param sourceRoots the set of source roots of this source group. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public JavaSourceGroup(JavaSourceGroupName groupName, Collection<? extends File> sourceRoots) {
        if (groupName == null) throw new NullPointerException("groupName");

        this.groupName = groupName;
        this.sourceRoots = Collections.unmodifiableSet(new LinkedHashSet<File>(sourceRoots));

        CollectionUtils.checkNoNullElements(this.sourceRoots, "sourceRoots");
    }

    /**
     * Returns the name of these source roots representing the type of sources
     * they contain. That is, this method returns the name of a particular
     * {@code org.gradle.api.file.SourceDirectorySet}. For example, in the code
     * below:
     * <pre>
     * sourceSets {
     *     main {
     *         java {
     *             // ...
     *         }
     *     }
     * }
     * </pre>
     * {@code JavaSourceGroupName.JAVA} could be a name of a source group.
     *
     * @return the name of these source roots representing the type of sources
     *   they contain. This method never returns {@code null}.
     */
    public JavaSourceGroupName getGroupName() {
        return groupName;
    }

    /**
     * Returns the set of source roots of this source group.
     *
     * @return the set of source roots of this source group. This method may
     *   never return {@code null} and the returned set does not contain
     *   {@code null} elements.
     */
    public Set<File> getSourceRoots() {
        return sourceRoots;
    }
}
