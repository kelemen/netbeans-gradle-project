package org.netbeans.gradle.project.model;

public final class GradleTaskID {
    private final String name;
    private final String fullName;

    public GradleTaskID(String name, String fullName) {
        this.name = name;
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }
}
