package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;

public final class ProjectDependencyCandidate {
    private final Project project;
    private final File dependency;

    private volatile JavaExtension javaExtCache;

    public ProjectDependencyCandidate(Project project, File dependency) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(dependency, "dependency");

        this.project = project;
        this.dependency = dependency;
        this.javaExtCache = null;
    }

    public Project getProject() {
        return project;
    }

    private JavaExtension tryGetJavaExt() {
        JavaExtension result = javaExtCache;
        if (result == null) {
            result = project.getLookup().lookup(JavaExtension.class);
            if (result != null) {
                javaExtCache = result;
            }
        }
        return result;
    }

    public File getDependency() {
        return dependency;
    }

    public JavaProjectDependencyDef tryGetDependency() {
        JavaExtension javaExt = tryGetJavaExt();
        if (javaExt == null) {
            return null;
        }

        NbJavaModule module = javaExt.getCurrentModel().getMainModule();
        List<JavaSourceSet> sourceSets = module.getSourceSetsForOutput(dependency);
        if (sourceSets.isEmpty()) {
            return null;
        }

        return new JavaProjectDependencyDef(javaExt, sourceSets);
    }

    @Override
    public String toString() {
        return "ProjectDependencyCandidate{" + dependency + '}';
    }
}
