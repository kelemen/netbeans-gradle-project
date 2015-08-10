package org.netbeans.gradle.project;

import java.io.File;
import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.WeakValueHashMap;
import org.netbeans.gradle.project.util.NbConsumer;
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

    public void forProjects(NbConsumer<? super NbGradleProject> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        for (NbGradleProject project: projects.values()) {
            action.accept(project);
        }
    }

    public NbGradleProject tryGetLoadedProject(File projectDir) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
        Path path = NbFileUtils.asPath(projectDir);
        if (path == null) {
            return null;
        }
        return tryGetLoadedProject(path);
    }

    public NbGradleProject tryGetLoadedProject(Path projectDir) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
        return projects.get(projectDir);
    }
}
