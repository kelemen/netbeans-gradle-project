package org.netbeans.gradle.project.properties.standard;

import java.util.List;
import java.util.Objects;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class CustomTasks {
    private final PredefinedTasks tasks;

    public CustomTasks(List<PredefinedTask> tasks) {
        this(new PredefinedTasks(tasks));
    }

    public CustomTasks(PredefinedTasks tasks) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
    }

    public List<PredefinedTask> getTasks() {
        return tasks.getTasks();
    }

    public PredefinedTasks getPredefinedTasks() {
        return tasks;
    }
}
