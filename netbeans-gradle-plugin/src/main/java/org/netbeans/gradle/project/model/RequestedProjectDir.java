package org.netbeans.gradle.project.model;

import java.io.File;

public final class RequestedProjectDir {
    private final File projectDir;

    public RequestedProjectDir(File projectDir) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        this.projectDir = projectDir;
    }

    public File getProjectDir() {
        return projectDir;
    }
}
