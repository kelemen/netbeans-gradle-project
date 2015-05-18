package org.netbeans.gradle.project.properties.standard;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;

public final class UserInitScriptPath {
    private final Path relPath;

    public UserInitScriptPath(Path relPath) {
        ExceptionHelper.checkNotNullArgument(relPath, "relPath");
        this.relPath = relPath;
    }

    public Path getPath(NbGradleProject project) {
        return project.currentModel().getValue().getSettingsDir().resolve(relPath);
    }

    public Path getRelPath() {
        return relPath;
    }
}
