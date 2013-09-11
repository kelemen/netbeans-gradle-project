package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@SuppressWarnings("serial") // don't care
public final class OpenProjectsAction extends AbstractAction {
    private static final Logger LOGGER = Logger.getLogger(OpenProjectsAction.class.getName());

    private final Collection<File> projectDirs;

    public static OpenProjectsAction createFromModules(
            String caption,
            Collection<? extends NbGradleProjectTree> projects) {
        Collection<File> projectDirs = new ArrayList<File>(projects.size());
        for (NbGradleProjectTree project: projects) {
            if (project == null) throw new NullPointerException("project");
            projectDirs.add(project.getProjectDir());
        }
        return new OpenProjectsAction(caption, projectDirs);
    }

    public static OpenProjectsAction createFromProjectDirs(
            String caption,
            Collection<File> projectDirs) {
        Collection<File> safeProjectDirs = new ArrayList<File>(projectDirs);
        for (File project: safeProjectDirs) {
            if (project == null) throw new NullPointerException("project");
        }
        return new OpenProjectsAction(caption, safeProjectDirs);
    }

    private OpenProjectsAction(String caption, Collection<File> projectDirs) {
        super(caption);
        this.projectDirs = projectDirs;
    }

    private void openProject(File projectDir) {
        LOGGER.log(Level.FINE, "Trying to open project: {0}", projectDir.getName());

        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        if (projectDirObj == null) {
            LOGGER.log(Level.WARNING,
                    "Directory of the project does not exist: {0}",
                    projectDir);
            return;
        }

        try {
            ProjectManager projectManager = ProjectManager.getDefault();

            Closeable safeToOpenKey = NbGradleProjectFactory.safeToOpen(projectDirObj);
            try {
                // We have to clear this list because if the project
                // does not have build.gradle, NetBeans might have
                // already determined that the directory does not
                // contain a project.
                projectManager.clearNonProjectCache();

                Project subProject = projectManager.findProject(projectDirObj);
                if (subProject == null) {
                    LOGGER.log(Level.WARNING,
                            "Project cannot be found: {0}",
                            projectDir);
                    return;
                }
                OpenProjects.getDefault().open(new Project[]{subProject}, false);

            } finally {
                safeToOpenKey.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Error while trying to load the project: " + projectDir,
                    ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                for (File projectDir: projectDirs) {
                    openProject(projectDir);
                }
            }
        });
    }
}
