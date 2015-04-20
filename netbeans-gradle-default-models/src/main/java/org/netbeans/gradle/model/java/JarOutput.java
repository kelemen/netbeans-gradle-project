package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class JarOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String taskName;
    private final File jar;
    private final Set<String> sourceSetNames;

    public JarOutput(String taskName, File jar, Set<String> sourceSetNames) {
        if (taskName == null) throw new NullPointerException("taskName");
        if (jar == null) throw new NullPointerException("jar");

        this.taskName = taskName;
        this.jar = jar;
        this.sourceSetNames = sourceSetNames != null ? copySet(sourceSetNames) : null;

        if (this.sourceSetNames != null) {
            CollectionUtils.checkNoNullElements(this.sourceSetNames, "sourceSetNames");
        }
    }

    private static <E> Set<E> copySet(Set<? extends E> src) {
        int srcSize = src.size();
        switch (srcSize) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(src.iterator().next());
            default:
                return Collections.unmodifiableSet(new HashSet<E>(src));
        }
    }

    public String getTaskName() {
        return taskName;
    }

    public File getJar() {
        return jar;
    }

    public Set<String> tryGetSourceSetNames() {
        return sourceSetNames;
    }
}
