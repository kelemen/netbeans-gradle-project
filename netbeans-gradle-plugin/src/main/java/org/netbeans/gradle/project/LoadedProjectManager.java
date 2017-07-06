package org.netbeans.gradle.project;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import org.netbeans.gradle.project.properties.WeakValueHashMap;
import org.netbeans.gradle.project.util.NbFileUtils;

public final class LoadedProjectManager {
    private static final LoadedProjectManager DEFAULT = new LoadedProjectManager();

    private final WeakValueHashMap<Path, NbGradleProject> projects;

    public LoadedProjectManager() {
        this.projects = new WeakValueHashMap<>();
    }

    public static LoadedProjectManager getDefault() {
        return DEFAULT;
    }

    public void addProject(NbGradleProject project) {
        Path projectDir = project.getProjectDirectoryAsPath();
        projects.put(projectDir, project);
    }

    public void forProjects(Consumer<? super NbGradleProject> action) {
        Objects.requireNonNull(action, "action");
        for (NbGradleProject project: projects.values()) {
            action.accept(project);
        }
    }

    public NbGradleProject tryGetLoadedProject(File projectDir) {
        Objects.requireNonNull(projectDir, "projectDir");
        Path path = NbFileUtils.asPath(projectDir);
        if (path == null) {
            return null;
        }
        return tryGetLoadedProject(path);
    }

    public NbGradleProject tryGetLoadedProject(Path projectDir) {
        Objects.requireNonNull(projectDir, "projectDir");
        return projects.get(projectDir);
    }
}
