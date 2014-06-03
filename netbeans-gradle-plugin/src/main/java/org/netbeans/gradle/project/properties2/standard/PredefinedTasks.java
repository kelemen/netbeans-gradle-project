package org.netbeans.gradle.project.properties2.standard;

import java.util.List;
import java.util.Objects;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class PredefinedTasks {
    private final List<PredefinedTask> tasks;

    public PredefinedTasks(List<PredefinedTask> tasks) {
        this.tasks = CollectionUtils.copyNullSafeList(tasks);
    }

    public List<PredefinedTask> getTasks() {
        return tasks;
    }

    @Override
    public int hashCode() {
        return 469 + Objects.hashCode(this.tasks);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;
        final PredefinedTasks other = (PredefinedTasks)obj;
        return Objects.equals(this.tasks, other.tasks);
    }
}
