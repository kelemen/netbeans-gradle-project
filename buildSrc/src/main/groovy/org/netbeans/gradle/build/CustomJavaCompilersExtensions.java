package org.netbeans.gradle.build;

import java.io.File;
import java.util.Objects;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

public final class CustomJavaCompilersExtensions {
    private final Project project;

    public CustomJavaCompilersExtensions(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public File getToolsJar() {
        JavaVersion javaVersion = CompilerUtils.getTargetCompatibility(project);
        return CompilerUtils.findToolsJar(project, javaVersion);
    }

    public void addCompilerArgs(JavaCompile task, String... newArgs) {
        addCompilerArgs(task.getOptions(), newArgs);
    }

    public void addCompilerArgs(CompileOptions options, String... newArgs) {
        CompilerUtils.addCompilerArgs(options, newArgs);
    }
}
