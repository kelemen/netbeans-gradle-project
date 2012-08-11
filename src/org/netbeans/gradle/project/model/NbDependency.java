package org.netbeans.gradle.project.model;

public interface NbDependency {
    public String getShortName();
    public boolean isTransitive();
}
