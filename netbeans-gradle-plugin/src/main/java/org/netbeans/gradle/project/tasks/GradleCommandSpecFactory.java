package org.netbeans.gradle.project.tasks;

public interface GradleCommandSpecFactory {
    public String getDisplayName();
    public GradleCommandSpec tryCreateCommandSpec() throws Exception;
}
