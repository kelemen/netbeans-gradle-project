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

    private final Set<String> excludes;
    private final Set<String> includes;

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
        this(groupName, sourceRoots, Collections.<String>emptySet(), Collections.<String>emptySet());
    }

    /**
     * Creates a new {@code JavaSourceGroup} with the given properties.
     *
     * @param groupName the name of these source roots representing the type of
     *   sources they contain. This argument cannot be {@code null}.
     * @param sourceRoots the set of source roots of this source group. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     * @param excludes the exclude patterns to be excluded from this source
     *   group. This argument cannot be {@code null}.
     * @param includes the include patterns to be included in this source
     *   group. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public JavaSourceGroup(
            JavaSourceGroupName groupName,
            Collection<? extends File> sourceRoots,
            Collection<? extends String> excludes,
            Collection<? extends String> includes) {
        if (groupName == null) throw new NullPointerException("groupName");

        this.groupName = groupName;
        this.sourceRoots = copySet(sourceRoots);
        this.excludes = copySet(excludes);
        this.includes = copySet(includes);

        CollectionUtils.checkNoNullElements(this.sourceRoots, "sourceRoots");
    }

    private static <T> Set<T> copySet(Collection<? extends T> src) {
        if (src.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(new LinkedHashSet<T>(src));
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

    /**
     * Returns the patterns of paths to be excluded from this source group.
     * Exclude patterns are applied after include patterns.
     *
     * @return the patterns of paths to be excluded from this source group.
     *   This method never returns {@code null}.
     */
    public Set<String> getExcludes() {
        // The null check is there for backward compatibility.
        // That is, when this object was serialized with a previous version
        // of this class.
        return excludes != null ? excludes : Collections.<String>emptySet();
    }

    /**
     * Returns the patterns of paths to be included in this source group.
     * Include patterns are applied before exclude patterns.
     *
     * @return the patterns of paths to be included in this source group.
     *   This method never returns {@code null}.
     */
    public Set<String> getIncludes() {
        // The null check is there for backward compatibility.
        // That is, when this object was serialized with a previous version
        // of this class.
        return includes != null ? includes : Collections.<String>emptySet();
    }
}
