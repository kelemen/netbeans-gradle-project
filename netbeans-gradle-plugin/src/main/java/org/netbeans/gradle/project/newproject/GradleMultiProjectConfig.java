package org.netbeans.gradle.project.newproject;

import java.io.File;

public final class GradleMultiProjectConfig {
    private final String projectName;
    private final File projectFolder;
    private final String mavenGroupId;
    private final String mavenVersion;

    public GradleMultiProjectConfig(
            String projectName,
            File projectFolder,
            String mavenGroupId,
            String mavenVersion) {
        if (projectName == null) throw new NullPointerException("projectName");
        if (projectFolder == null) throw new NullPointerException("projectFolder");

        this.projectName = projectName;
        this.projectFolder = projectFolder;
        this.mavenGroupId = mavenGroupId;
        this.mavenVersion = mavenVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public File getProjectFolder() {
        return projectFolder;
    }

    public String getMavenGroupId() {
        return mavenGroupId;
    }

    public String getMavenVersion() {
        return mavenVersion;
    }
}
