package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class PersistentModelKey {
    private final Path rootPath;
    private final Path projectDir;

    public PersistentModelKey(NbGradleModel model) {
        this(model.getSettingsDir(), model.getProjectDir().toPath());
    }

    public PersistentModelKey(Path rootPath, Path projectDir) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath");
        this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path getProjectDir() {
        return projectDir;
    }

    public PersistentModelKey normalize() throws IOException {
        Path newRootPath = NbFileUtils.toSafeRealPath(rootPath);
        Path newProjectDir = NbFileUtils.toSafeRealPath(projectDir);

        if (rootPath.equals(newRootPath) && projectDir.equals(newProjectDir)) {
            return this;
        }
        else {
            return new PersistentModelKey(newRootPath, newProjectDir);
        }
    }

    @Override
    public String toString() {
        return "PersistentModelKey{" + "rootPath=" + rootPath + ", projectDir=" + projectDir + '}';
    }
}
