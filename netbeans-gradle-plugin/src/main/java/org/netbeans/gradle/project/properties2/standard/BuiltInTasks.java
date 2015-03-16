package org.netbeans.gradle.project.properties2.standard;

import org.netbeans.gradle.project.properties.PredefinedTask;

public interface BuiltInTasks {
    public PredefinedTasks getAllTasks();
    public PredefinedTask tryGetByCommand(String command);
}
