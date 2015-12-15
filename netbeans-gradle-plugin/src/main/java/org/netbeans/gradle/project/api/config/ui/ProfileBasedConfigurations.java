package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.properties.ProfileBasedCustomizer;
import org.netbeans.gradle.project.properties.ProfileBasedPanel;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;

public final class ProfileBasedConfigurations {
    public static ProjectCustomizer.CompositeCategoryProvider createProfileBasedCustomizer(
            @Nonnull final Project project,
            @Nonnull final CustomizerCategoryId categoryId,
            @Nonnull final ProjectSettingsProvider.ExtensionSettings extensionSettings,
            @Nonnull final ProfileBasedProjectSettingsPageFactory pageFactory) {

        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(categoryId, "categoryId");
        ExceptionHelper.checkNotNullArgument(extensionSettings, "extensionSettings");
        ExceptionHelper.checkNotNullArgument(pageFactory, "pageFactory");

        ProfileBasedCustomizer.PanelFactory panelFactory = new ProfileBasedCustomizer.PanelFactory() {
            @Override
            public ProfileBasedPanel createPanel() {
                return createProfileBasedPanel(project, extensionSettings, pageFactory);
            }
        };

        return new ProfileBasedCustomizer(categoryId.getCategoryName(), categoryId.getDisplayName(), panelFactory);
    }

    private static ProfileBasedPanel createProfileBasedPanel(
            Project project,
            ProjectSettingsProvider.ExtensionSettings extensionSettings,
            ProfileBasedProjectSettingsPageFactory pageFactory) {

        ProfileBasedProjectSettingsPage settingsPage = pageFactory.createSettingsPage();
        JComponent customPanel = settingsPage.getSettingsPanel();
        ProfileValuesEditorFactory editorFactory = settingsPage.getEditorFactory();

        return ProfileBasedPanel.createPanel(project, extensionSettings, customPanel, editorFactory);
    }

    private ProfileBasedConfigurations() {
        throw new AssertionError();
    }
}
