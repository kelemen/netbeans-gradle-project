package org.netbeans.gradle.project.java.model;

import java.io.Serializable;

public final class JavaProjectDependency implements Serializable {
    private static final long serialVersionUID = 1L;

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
