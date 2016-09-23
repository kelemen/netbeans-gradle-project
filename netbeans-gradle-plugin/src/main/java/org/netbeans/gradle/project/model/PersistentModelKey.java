package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;

public final class PersistentModelKey {
    private final Path rootPath;
    private final Path projectDir;

    public PersistentModelKey(Path rootPath, Path projectDir) {
        ExceptionHelper.checkNotNullArgument(rootPath, "rootPath");
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");

        this.rootPath = rootPath;
        this.projectDir = projectDir;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path getProjectDir() {
        return projectDir;
    }

    @Override
    public String toString() {
        return "PersistentModelKey{" + "rootPath=" + rootPath + ", projectDir=" + projectDir + '}';
    }
}
