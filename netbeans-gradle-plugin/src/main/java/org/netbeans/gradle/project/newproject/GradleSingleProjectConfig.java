package org.netbeans.gradle.project.newproject;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;

public final class GradleSingleProjectConfig {
    private final String projectName;
    private final Path projectFolder;
    private final String mainClass;

    public GradleSingleProjectConfig(
            String projectName,
            Path projectFolder,
            String mainClass) {
        ExceptionHelper.checkNotNullArgument(projectName, "projectName");
        ExceptionHelper.checkNotNullArgument(projectFolder, "projectFolder");

        this.projectName = projectName;
        this.projectFolder = projectFolder;
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
