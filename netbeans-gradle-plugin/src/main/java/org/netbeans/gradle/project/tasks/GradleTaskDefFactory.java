package org.netbeans.gradle.project.tasks;

public interface GradleTaskDefFactory {
    public String getDisplayName();
    public GradleTaskDef tryCreateTaskDef() throws Exception;
}
