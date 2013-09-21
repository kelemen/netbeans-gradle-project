package org.netbeans.gradle.project.tasks;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.GradleModelLoader;

public final class DownloadSourcesTask implements DaemonTask {
    private final NbGradleProject project;

    public DownloadSourcesTask(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    public static DaemonTaskDef createTaskDef(NbGradleProject project) {
        return new DaemonTaskDef("Downloading sources", true, new DownloadSourcesTask(project));
    }

    @Override
    public void run(ProgressHandle progress) {
        GradleConnector connector = GradleModelLoader.createGradleConnector(project);
        connector.forProjectDirectory(project.getProjectDirectoryAsFile());

        OperationInitializer setup = GradleModelLoader.modelBuilderSetup(project, progress);

        // FIXME: Currently we just fetch IdeaProject and rely on that to fetch
        //   the sources. Then the source locator query will find the sources
        //   in the Gradle cache.
        ProjectConnection connection = connector.connect();
        try {
            ModelBuilder<IdeaProject> builder = connection.model(IdeaProject.class);
            GradleModelLoader.setupLongRunningOP(setup, builder);

            builder.get();
        } finally {
            connection.close();
        }
    }
}
