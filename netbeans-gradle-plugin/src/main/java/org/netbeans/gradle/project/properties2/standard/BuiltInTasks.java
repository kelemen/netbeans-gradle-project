package org.netbeans.gradle.project.properties2.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.PredefinedTask;

public final class BuiltInTasks {
    private final PredefinedTasks tasks;
    private final Map<String, PredefinedTask> nameToTask;

    public BuiltInTasks(Collection<PredefinedTask> tasks) {
        this(new PredefinedTasks(tasks));
    }

    public BuiltInTasks(PredefinedTasks tasks) {
        this(tasks, createNameToTask(tasks.getTasks()));
    }

    private BuiltInTasks(PredefinedTasks tasks, Map<String, PredefinedTask> nameToTask) {
        ExceptionHelper.checkNotNullArgument(tasks, "tasks");
        assert nameToTask != null;

        this.tasks = tasks;
        this.nameToTask = nameToTask;
    }

    private static Map<String, PredefinedTask> createNameToTask(Collection<PredefinedTask> tasks) {
        Map<String, PredefinedTask> result = CollectionsEx.newHashMap(tasks.size());
        for (PredefinedTask task: tasks) {
            result.put(task.getDisplayName(), task);
        }

        return Collections.unmodifiableMap(result);
    }

    public PredefinedTask tryGetByCommand(String command) {
        ExceptionHelper.checkNotNullArgument(command, "command");
        return nameToTask.get(command);
    }

    public List<PredefinedTask> getTasks() {
        return tasks.getTasks();
    }

    public PredefinedTasks getPredefinedTasks() {
        return tasks;
    }

    public BuiltInTasks inheritFrom(BuiltInTasks parent) {
        ExceptionHelper.checkNotNullArgument(parent, "parent");

        Map<String, PredefinedTask> combinedNameToTask = CollectionsEx
                .newHashMap(Math.max(parent.nameToTask.size(), nameToTask.size()));

        combinedNameToTask.putAll(parent.nameToTask);
        combinedNameToTask.putAll(nameToTask);

        PredefinedTasks combinedTasks = new PredefinedTasks(new ArrayList<>(combinedNameToTask.values()));
        return new BuiltInTasks(combinedTasks, Collections.unmodifiableMap(combinedNameToTask));
    }
}
