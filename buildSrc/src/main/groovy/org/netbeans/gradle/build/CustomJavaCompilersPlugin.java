package org.netbeans.gradle.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class CustomJavaCompilersPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().add("customJavaCompilers", new CustomJavaCompilersExtensions(project));
        CompilerUtils.configureJavaCompilers(project);
    }
}
