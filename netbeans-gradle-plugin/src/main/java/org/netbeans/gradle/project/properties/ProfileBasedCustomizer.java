package org.netbeans.gradle.project.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

public final class ProfileBasedCustomizer implements ProjectCustomizer.CompositeCategoryProvider {
    private final String categoryName;
    private final String displayName;
    private final ProfileBasedPanel panel;

    public ProfileBasedCustomizer(String categoryName, String displayName, ProfileBasedPanel panel) {
        ExceptionHelper.checkNotNullArgument(categoryName, "categoryName");
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");
        ExceptionHelper.checkNotNullArgument(panel, "panel");

        this.categoryName = categoryName;
        this.displayName = displayName;
        this.panel = panel;
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(categoryName, displayName, null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        category.setOkButtonListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.saveProperties();
            }
        });
        return panel;
    }
}
