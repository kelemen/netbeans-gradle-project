package org.netbeans.gradle.project.properties;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.HelpCtx;

public final class GradleCustomizer implements CustomizerProvider {
    private static final String GRADLE_CATEGORY_NAME = "gradle";

    private static final ProjectCustomizer.Category GRADLE_CATEGORY
            = ProjectCustomizer.Category.create(GRADLE_CATEGORY_NAME, NbStrings.getGradleProjectCategoryName(), null);

    private final NbGradleProject project;

    public GradleCustomizer(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public void showCustomizer() {
        ProjectCustomizer.Category[] categories = new ProjectCustomizer.Category[] {
            GRADLE_CATEGORY
        };

        final ProjectPropertiesPanel panel = new ProjectPropertiesPanel(project);

        ProjectCustomizer.CategoryComponentProvider panelProvider = new ProjectCustomizer.CategoryComponentProvider() {
            @Override
            public JComponent create(ProjectCustomizer.Category category) {
                String name = category.getName();
                if (GRADLE_CATEGORY_NAME.equals(name)) {
                    return panel;
                }

                return new JPanel();
            }
        };

        ActionListener okListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.saveProperties();
            }
        };

        Dialog dlg = ProjectCustomizer.createCustomizerDialog(
                categories,
                panelProvider,
                GRADLE_CATEGORY_NAME,
                okListener,
                HelpCtx.DEFAULT_HELP);

        dlg.setTitle(NbStrings.getProjectPropertiesDlgTitle(project.getDisplayName()));
        dlg.setModal(true);
        dlg.setVisible(true);
    }
}
