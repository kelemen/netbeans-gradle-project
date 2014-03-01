package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Kelemen Attila
 */
public final class DeleteProjectAction extends AbstractAction {
    private static final long serialVersionUID = -386797460711624644L;

    private static final Logger LOGGER = Logger.getLogger(DeleteProjectAction.class.getName());
    private static final RequestProcessor PROJECT_PROCESSOR
            = new RequestProcessor("Delete-Project-Processor", 1, true);

    private final NbGradleProject project;

    public DeleteProjectAction(NbGradleProject project) {
        super(NbStrings.getDeleteProjectCaption());

        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private void closeAffectedProjects() {
        OpenProjects openProjects = OpenProjects.getDefault();
        FileObject rootDir = project.getProjectDirectory();

        List<Project> toClose = new LinkedList<>();
        toClose.add(project);
        for (Project opened: openProjects.getOpenProjects()) {
            if (FileUtil.isParentOf(rootDir, opened.getProjectDirectory())) {
                toClose.add(project);
            }
        }

        openProjects.close(toClose.toArray(new Project[0]));
    }

    private void doRemoveProject() {
        try {
            closeAffectedProjects();
            project.getProjectDirectory().delete();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "There was an error while trying to remove the project.", ex);
            String title = NbStrings.getErrorDeleteProjectTitle();
            String message = NbStrings.getErrorDeleteProject(ex);
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object[] answers = {NbStrings.getYesOption(), NbStrings.getNoOption()};
        String message = NbStrings.getConfirmDeleteProject(project.getDisplayName());
        String title = NbStrings.getConfirmDeleteProjectTitle();
        if (JOptionPane.showOptionDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, answers, answers[1]) != 0) {
            return;
        }

        final ProgressHandle progress
                = ProgressHandleFactory.createHandle(NbStrings.getDeleteProjectProgress(project.getDisplayName()));
        progress.start();
        PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doRemoveProject();
                } finally {
                    progress.finish();
                }
            }
        });
    }
}
