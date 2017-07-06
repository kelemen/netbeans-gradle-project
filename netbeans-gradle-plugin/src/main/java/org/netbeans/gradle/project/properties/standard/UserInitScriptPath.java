package org.netbeans.gradle.project.properties.standard;

import java.nio.file.Path;
import java.util.Objects;
import org.netbeans.gradle.project.NbGradleProject;

public final class UserInitScriptPath {
    private final Path relPath;

    public UserInitScriptPath(Path relPath) {
        this.relPath = Objects.requireNonNull(relPath, "relPath");
    }

    public Path getPath(NbGradleProject project) {
        return project.currentModel().getValue().getSettingsDir().resolve(relPath);
    }

    public Path getRelPath() {
        return relPath;
    }
}
