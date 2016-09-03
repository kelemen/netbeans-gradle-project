package org.netbeans.gradle.project.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.ui.ProfileBasedPanel;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

public final class ProfileBasedCustomizer implements ProjectCustomizer.CompositeCategoryProvider {
    private final String categoryName;
    private final String displayName;
    private final PanelFactory panelFactory;

    public ProfileBasedCustomizer(String categoryName, String displayName, final ProfileBasedPanel panel) {
        this(categoryName, displayName, new PanelFactory() {
            @Override
            public ProfileBasedPanel createPanel() {
                return panel;
            }
        });

        ExceptionHelper.checkNotNullArgument(panel, "panel");
    }

    public ProfileBasedCustomizer(String categoryName, String displayName, PanelFactory panelFactory) {
        ExceptionHelper.checkNotNullArgument(categoryName, "categoryName");
        ExceptionHelper.checkNotNullArgument(displayName, "displayName");
        ExceptionHelper.checkNotNullArgument(panelFactory, "panelFactory");

        this.categoryName = categoryName;
        this.displayName = displayName;
        this.panelFactory = panelFactory;
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(categoryName, displayName, null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        final ProfileBasedPanel panel = panelFactory.createPanel();
        category.setOkButtonListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.saveProperties();
            }
        });
        return panel;
    }

    public static interface PanelFactory {
        public ProfileBasedPanel createPanel();
    }
}
