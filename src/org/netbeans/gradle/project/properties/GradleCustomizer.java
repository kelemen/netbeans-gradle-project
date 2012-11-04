package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

public final class GradleCustomizer implements CustomizerProvider {
    private final NbGradleProject project;

    public GradleCustomizer(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public void showCustomizer() {
        ProjectPropertiesPanel panel = new ProjectPropertiesPanel(project);

        DialogDescriptor dlgDescriptor = new DialogDescriptor(
                panel,
                NbStrings.getProjectPropertiesDlgTitle(project.getDisplayName()),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                null);

        Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
        dlg.setVisible(true);
        if (DialogDescriptor.OK_OPTION == dlgDescriptor.getValue()) {
            panel.saveProperties();
        }
    }
}
