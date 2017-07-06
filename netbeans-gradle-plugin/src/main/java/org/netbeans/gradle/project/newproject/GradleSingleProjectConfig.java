package org.netbeans.gradle.project.newproject;

import java.nio.file.Path;
import java.util.Objects;

public final class GradleSingleProjectConfig {
    private final String projectName;
    private final Path projectFolder;
    private final String mainClass;

    public GradleSingleProjectConfig(
            String projectName,
            Path projectFolder,
            String mainClass) {
        this.projectName = Objects.requireNonNull(projectName, "projectName");
        this.projectFolder = Objects.requireNonNull(projectFolder, "projectFolder");
        this.mainClass = mainClass;
    }

    public String getProjectName() {
        return projectName;
    }

    public Path getProjectFolder() {
        return projectFolder;
    }

    public String getMainClass() {
        return mainClass;
    }
}
