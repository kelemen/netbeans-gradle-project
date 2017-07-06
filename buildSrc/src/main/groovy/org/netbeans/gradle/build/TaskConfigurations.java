package org.netbeans.gradle.build;

import groovy.lang.Closure;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.Task;

public final class TaskConfigurations {
    public static Task lazilyConfiguredTask(final Task task, final Action<? super Task> taskConfiguration) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(taskConfiguration, "taskConfiguration");

        String configTaskName = "configure" + capitalizeFirst(task.getName());
        Task configTask = task.getProject().getTasks().findByName(configTaskName);
        if (configTask == null) {
            configTask = task.getProject().task(configTaskName);
            task.dependsOn(configTask);
        }

        configTask.doLast(t -> {
            taskConfiguration.execute(task);
        });

        return task;
    }

    public static Task lazilyConfiguredTask(Task task, final Closure<?> taskConfiguration) {
        Objects.requireNonNull(taskConfiguration, "taskConfiguration");

        return lazilyConfiguredTask(task, (Task configuredTask) -> {
            taskConfiguration.setDelegate(configuredTask);
            taskConfiguration.setResolveStrategy(Closure.DELEGATE_FIRST);
            taskConfiguration.call(configuredTask);
        });
    }

    public static String capitalizeFirst(String str) {
        if (str.isEmpty()) {
            return str;
        }

        char firstCh = str.charAt(0);
        char newFirstCh = Character.toUpperCase(firstCh);
        if (firstCh == newFirstCh) {
            return str;
        }

        return newFirstCh + str.substring(1);
    }

    private TaskConfigurations() {
        throw new AssertionError();
    }
}
