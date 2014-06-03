package org.netbeans.gradle.project.properties2.standard;

import java.util.List;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class CustomTasks {
    private final PredefinedTasks tasks;

    public CustomTasks(List<PredefinedTask> tasks) {
        this(new PredefinedTasks(tasks));
    }

    public CustomTasks(PredefinedTasks tasks) {
        ExceptionHelper.checkNotNullArgument(tasks, "tasks");
        this.tasks = tasks;
    }

    public List<PredefinedTask> getTasks() {
        return tasks.getTasks();
    }

    public PredefinedTasks getPredefinedTasks() {
        return tasks;
    }
}
