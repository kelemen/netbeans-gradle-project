package org.netbeans.gradle.project.java.query;

import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.GradleProjectContextActions;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;

public final class JavaProjectContextActions implements GradleProjectContextActions {
    public JavaProjectContextActions(JavaExtension javaExt) {
    }

    private static Action createProjectAction(String command, String label) {
        return ProjectSensitiveActions.projectCommandAction(command, label, null);
    }

    @Override
    public List<Action> getContextActions() {
        return Arrays.asList(createProjectAction(
                JavaProjectConstants.COMMAND_JAVADOC,
                NbStrings.getJavadocCommandCaption()));
    }
}
