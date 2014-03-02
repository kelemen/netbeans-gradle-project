package org.netbeans.gradle.project.tasks;

public interface DaemonTaskDefFactory {
    public String getDisplayName();
    public DaemonTaskDef tryCreateTaskDef() throws Exception;
}
