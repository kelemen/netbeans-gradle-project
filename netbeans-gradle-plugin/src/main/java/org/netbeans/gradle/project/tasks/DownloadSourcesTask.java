package org.netbeans.gradle.project.tasks;

import java.io.File;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class DownloadSourcesTask implements DaemonTask {
    private final Project project;

    public DownloadSourcesTask(Project project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    public static DaemonTaskDef createTaskDef(NbGradleProject project) {
        return new DaemonTaskDef(
                NbStrings.getDownloadSourcesProgressCaption(),
                true,
                new DownloadSourcesTask(project));
    }

    @Override
    public void run(CancellationToken cancelToken, ProgressHandle progress) {
        GradleConnector connector = GradleModelLoader.createGradleConnector(cancelToken, project);
        FileObject projectDirObj = project.getProjectDirectory();
        File projectDir = FileUtil.toFile(projectDirObj);
        if (projectDir == null) {
            throw new RuntimeException("Missing project directory: " + projectDirObj);
        }

        connector.forProjectDirectory(projectDir);

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
