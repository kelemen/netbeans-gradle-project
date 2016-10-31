package org.netbeans.gradle.build;

import java.io.File;
import java.util.Objects;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.plugins.nbm.CompilerUtils;

public final class CustomJavaCompilersExtensions {
    private final Project project;

    public CustomJavaCompilersExtensions(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public File getToolsJar() {
        JavaVersion javaVersion = CompilerUtils.getTargetCompatibility(project);
        return CompilerUtils.findToolsJar(project, javaVersion);
    }
}
