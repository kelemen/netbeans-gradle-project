package org.netbeans.gradle.project.properties.standard;

import org.netbeans.gradle.project.properties.PredefinedTask;

public interface BuiltInTasks {
    public PredefinedTasks getAllTasks();
    public PredefinedTask tryGetByCommand(String command);
}
