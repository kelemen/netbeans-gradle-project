package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a settings page for an extension. The settings page will support storing its properties into any profiles.
 * Extensions may provide instances of this class on their extension lookup
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * The settings are saved under the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#getName() extension name}
 * with respect to the argument of
 * {@link org.netbeans.gradle.project.api.config.ProjectSettingsProvider#getExtensionSettings(String) ProjectSettingsProvider.getExtensionSettings}
 * by default. If you need to overwrite this default (due to backward compatibility reasons), you must provide an
 * instance of {@link org.netbeans.gradle.project.api.config.ExtensionSettingsId ExtensionSettingsId} on the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#getLookup() lookup of GradleProjectExtensionDef}.
 */
public final class ProfileBasedSettingsCategory {
    private final CustomizerCategoryId categoryId;
    private final ProfileBasedSettingsPageFactory settingsPageFactory;

    /**
     * Creates a new settings page definition.
     *
     * @param categoryId the name of this settings page. This argument cannot be {@code null}.
     * @param settingsPageFactory the factory creating the component and the editor for the
     *   properties. This argument cannot be {@code null}.
     */
    public ProfileBasedSettingsCategory(
            @Nonnull CustomizerCategoryId categoryId,
            @Nonnull ProfileBasedSettingsPageFactory settingsPageFactory) {
        ExceptionHelper.checkNotNullArgument(categoryId, "categoryId");
        ExceptionHelper.checkNotNullArgument(settingsPageFactory, "settingsPageFactory");

        this.categoryId = categoryId;
        this.settingsPageFactory = settingsPageFactory;
    }

    /**
     * Returns the name (programmatic and display) of the settings page.
     *
     * @return the name (programmatic and display) of the settings page. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public CustomizerCategoryId getCategoryId() {
        return categoryId;
    }

    /**
     * Return the factory creating the component and the editor for the properties.
     *
     * @return the factory creating the component and the editor for the properties.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public ProfileBasedSettingsPageFactory getSettingsPageFactory() {
        return settingsPageFactory;
    }
}
