package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.io.Closeable;
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
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@SuppressWarnings("serial") // don't care
public final class OpenProjectsAction extends AbstractAction {
    private static final Logger LOGGER = Logger.getLogger(OpenProjectsAction.class.getName());

    private final Collection<NbGradleModule> projects;

    public OpenProjectsAction(String caption, Collection<NbGradleModule> projects) {
        super(caption);
        this.projects = new ArrayList<NbGradleModule>(projects);
        for (NbGradleModule project: this.projects) {
            if (project == null) throw new NullPointerException("project");
        }
    }

    public OpenProjectsAction(Collection<NbGradleModule> projects) {
        this(NbStrings.getOpenSubProjectCaption(projects), projects);
    }

    private void openSubProject(NbGradleModule project) {
        LOGGER.log(Level.FINE, "Trying to open project: {0}", project.getName());

        FileObject subProjectDir = FileUtil.toFileObject(project.getModuleDir());
        if (subProjectDir == null) {
            LOGGER.log(Level.WARNING,
                    "Directory of the project does not exist: {0}",
                    project.getModuleDir());
            return;
        }

        try {
            ProjectManager projectManager = ProjectManager.getDefault();

            Closeable safeToOpenKey = NbGradleProjectFactory.safeToOpen(subProjectDir);
            try {
                // We have to clear this list because if the project
                // does not have build.gradle, NetBeans might have
                // already determined that the directory does not
                // contain a project.
                projectManager.clearNonProjectCache();

                Project subProject = projectManager.findProject(subProjectDir);
                if (subProject == null) {
                    LOGGER.log(Level.WARNING,
                            "Project cannot be found: {0}",
                            project.getModuleDir());
                    return;
                }
                OpenProjects.getDefault().open(new Project[]{subProject}, false);

            } finally {
                safeToOpenKey.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Error while trying to load the project: " + project.getModuleDir(),
                    ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                for (NbGradleModule project: projects) {
                    openSubProject(project);
                }
            }
        });
    }
}
