package org.netbeans.gradle.project.java.model;

import java.io.Serializable;
import org.jtrim.utils.ExceptionHelper;

public final class JavaProjectDependency implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sourceSetName;
    private final JavaProjectReference projectReference;

    public JavaProjectDependency(String sourceSetName, JavaProjectReference projectReference) {
        ExceptionHelper.checkNotNullArgument(sourceSetName, "sourceSetName");
        ExceptionHelper.checkNotNullArgument(projectReference, "projectReference");

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
