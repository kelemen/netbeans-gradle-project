package org.netbeans.gradle.project;

import java.nio.file.Path;
import org.netbeans.gradle.project.properties.WeakValueHashMap;
import org.netbeans.gradle.project.util.NbConsumer;

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
        Path projectDir = project.getProjectDirectoryAsFile().toPath();
        projects.put(projectDir, project);
    }

    public void forProjects(NbConsumer<? super NbGradleProject> action) {
        for (NbGradleProject project: projects.values()) {
            action.accept(project);
        }
    }
}
