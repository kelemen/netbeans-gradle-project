package org.netbeans.gradle.project.api.config.ui;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Defines a settings page for an extension. The settings page will support storing its properties into any profiles.
 * Extensions may provide instances of this class on their extension lookup
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * The settings are saved under the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#getName() extension name}
 * with respect to the argument of
 * {@link org.netbeans.gradle.project.api.config.ProjectSettingsProvider#getExtensionSettings(String) ProjectSettingsProvider.getExtensionSettings}.
 * If you need to save settings under a different name (due to backward compatibility reasons), you must use a
 * {@link ProfileBasedForeignSettingsCategory} instead.
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
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.settingsPageFactory = Objects.requireNonNull(settingsPageFactory, "settingsPageFactory");
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
