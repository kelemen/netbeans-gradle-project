package org.netbeans.gradle.build;

import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

public final class NbGradleNbmPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.apply(Collections.singletonMap("plugin", "cz.kubacki.nbm"));

        TaskContainer tasks = project.getTasks();

        final Jar jar = (Jar)tasks.findByName("jar");
        TaskConfigurations.lazilyConfiguredTask(jar, task -> {
            jar.getManifest().attributes(Collections.singletonMap("Build-Jdk", getCompilerVersion(project)));
        });

        tasks.create("releaseToGithub", ReleaseToGithubTask.class);
        tasks.create("uploadDeployedToGitHub", UploadDeployedToGitHubTask.class);
    }

    private static String getCompilerVersion(Project project) {
        String result = CompilerUtils.tryGetCompilerVersion((JavaCompile)project.getTasks().findByName("compileJava"));
        return result != null ? result : "unknown";
    }
}
