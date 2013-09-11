package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Collection;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class GradleProjectTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties genericProperties;
    private final Collection<GradleTaskID> tasks;
    private final Collection<GradleProjectTree> children;

    public GradleProjectTree(
            GenericProjectProperties genericProperties,
            Collection<GradleTaskID> tasks,
            Collection<GradleProjectTree> children) {
        if (genericProperties == null) throw new NullPointerException("genericProperties");

        this.genericProperties = genericProperties;
        this.tasks = CollectionUtils.copyNullSafeList(tasks);
        this.children = CollectionUtils.copyNullSafeList(children);
    }

    public GenericProjectProperties getGenericProperties() {
        return genericProperties;
    }

    public Collection<GradleTaskID> getTasks() {
        return tasks;
    }

    public Collection<GradleProjectTree> getChildren() {
        return children;
    }
}
