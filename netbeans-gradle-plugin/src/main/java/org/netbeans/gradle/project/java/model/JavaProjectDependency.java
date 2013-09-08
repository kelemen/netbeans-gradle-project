package org.netbeans.gradle.project.java.model;

public final class JavaProjectDependency {
    private final String sourceSetName;
    private final JavaProjectReference projectReference;

    public JavaProjectDependency(String sourceSetName, JavaProjectReference projectReference) {
        if (sourceSetName == null) throw new NullPointerException("sourceSetName");
        if (projectReference == null) throw new NullPointerException("projectReference");

        this.sourceSetName = sourceSetName;
        this.projectReference = projectReference;
    }

    public String getSourceSetName() {
        return sourceSetName;
    }

    public JavaProjectReference getProjectReference() {
        return projectReference;
    }

    public NbJavaModule tryGetModule() {
        return projectReference.tryGetModule();
    }
}
