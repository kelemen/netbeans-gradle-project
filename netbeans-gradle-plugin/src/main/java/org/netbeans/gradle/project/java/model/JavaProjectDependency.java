package org.netbeans.gradle.project.java.model;

import java.io.Serializable;
import java.util.Objects;
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.sourceSetName);
        hash = 17 * hash + Objects.hashCode(this.projectReference);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final JavaProjectDependency other = (JavaProjectDependency)obj;
        return Objects.equals(this.sourceSetName, other.sourceSetName)
                && Objects.equals(this.projectReference, other.projectReference);
    }
}
