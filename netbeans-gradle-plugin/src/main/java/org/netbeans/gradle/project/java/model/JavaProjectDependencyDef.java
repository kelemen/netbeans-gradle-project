package org.netbeans.gradle.project.java.model;

import java.util.List;
import java.util.Objects;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;

public final class JavaProjectDependencyDef {
    private final JavaExtension javaExt;
    private final List<JavaSourceSet> sourceSets;

    public JavaProjectDependencyDef(JavaExtension javaExt, List<JavaSourceSet> sourceSets) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.sourceSets = CollectionsEx.readOnlyCopy(sourceSets);

        ExceptionHelper.checkNotNullElements(this.sourceSets, "sourceSets");
    }

    public Project getProject() {
        return javaExt.getProject();
    }

    public JavaExtension getJavaExt() {
        return javaExt;
    }

    public NbJavaModule getJavaModule() {
        return javaExt.getCurrentModel().getMainModule();
    }

    public List<JavaSourceSet> getSourceSets() {
        return sourceSets;
    }

    public String getDisplaySourceSetNames() {
        StringBuilder result = new StringBuilder();
        for (JavaSourceSet sourceSet: sourceSets) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(sourceSet.getName());
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "Project dependency of " + javaExt.getProjectDirectoryAsFile() + " for source sets: " + getDisplaySourceSetNames() + '}';
    }
}
