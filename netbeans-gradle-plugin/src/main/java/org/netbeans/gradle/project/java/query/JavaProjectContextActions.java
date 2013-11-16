package org.netbeans.gradle.project.java.query;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.GradleActionType;
import org.netbeans.gradle.project.api.nodes.GradleProjectAction;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;

public final class JavaProjectContextActions implements GradleProjectContextActions {
    private final JavaExtension javaExt;

    public JavaProjectContextActions(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    @Override
    public List<Action> getContextActions() {
        return Arrays.asList(
                createJavaDocAction(),
                createSourceDirsAction());
    }

    private Action createJavaDocAction() {
        return createProjectAction(
                JavaProjectConstants.COMMAND_JAVADOC,
                NbStrings.getJavadocCommandCaption());
    }

    private Action createSourceDirsAction() {
        return new CreateSourceDirsAction(NbStrings.getCreateSourceDirsAction());
    }

    @GradleProjectAction(GradleActionType.PROJECT_MANAGEMENT_ACTION)
    private class CreateSourceDirsAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public CreateSourceDirsAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    javaExt.getSourceDirsHandler().createDirectories();
                }
            });
        }
    }
}
