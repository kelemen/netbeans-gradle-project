package org.netbeans.gradle.project.newproject;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;

public final class GradleMultiProjectConfig {
    private final String projectName;
    private final Path projectFolder;
    private final String mavenGroupId;
    private final String mavenVersion;

    public GradleMultiProjectConfig(
            String projectName,
            Path projectFolder,
            String mavenGroupId,
            String mavenVersion) {
        ExceptionHelper.checkNotNullArgument(projectName, "projectName");
        ExceptionHelper.checkNotNullArgument(projectFolder, "projectFolder");

        this.projectName = projectName;
        this.projectFolder = projectFolder;
        this.mavenGroupId = mavenGroupId;
        this.mavenVersion = mavenVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public Path getProjectFolder() {
        return projectFolder;
    }

    public String getMavenGroupId() {
        return mavenGroupId;
    }

    public String getMavenVersion() {
        return mavenVersion;
    }
}
