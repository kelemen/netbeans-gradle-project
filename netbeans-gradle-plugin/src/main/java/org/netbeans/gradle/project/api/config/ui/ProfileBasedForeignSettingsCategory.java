package org.netbeans.gradle.project.api.config.ui;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Defines a settings page for an extension with properties stored under a custom name. The settings page will support
 * storing its properties into any profiles. Extensions may provide instances of this class on their extension lookup
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * The custom name must be the same what is passed as an argument to
 * {@link org.netbeans.gradle.project.api.config.ProjectSettingsProvider#getExtensionSettings(String) ProjectSettingsProvider.getExtensionSettings}
 * <P>
 * <B>Note</B>: Extensions are recommended to use {@link ProfileBasedSettingsCategory} instead which will store
 * the settings under the name of the extension. This ({@code ProfileBasedForeignSettingsCategory}) should only be
 * used for backward compatibility reasons.
 *
 * @see ProfileBasedSettingsCategory
 * @see org.netbeans.gradle.project.api.config.ProjectSettingsProvider#getExtensionSettings(String) ProjectSettingsProvider.getExtensionSettings
 */
public final class ProfileBasedForeignSettingsCategory {
    private final String extensionName;
    private final ProfileBasedSettingsCategory settingsCetegory;

    /**
     * Creates a settings page definition with properties stored under a custom name.
     *
     * @param extensionName the name under where the properties are saved. This is the same as the argument passed to
     *   {@code ProjectSettingsProvider.getExtensionSettings}. This argument cannot be {@code null}.
     * @param settingsCategory the definition of the settings page. This argument cannot be {@code null}.
     */
    public ProfileBasedForeignSettingsCategory(
            @Nonnull String extensionName,
            @Nonnull ProfileBasedSettingsCategory settingsCategory) {
        this.extensionName = Objects.requireNonNull(extensionName, "extensionName");
        this.settingsCetegory = Objects.requireNonNull(settingsCategory, "settingsCategory");
    }

    /**
     * Returns the name of the extension for the purpose of where to store the properties.
     * This is the same as the argument passed to {@code ProjectSettingsProvider.getExtensionSettings}.
     *
     * @return the name of the extension for the purpose of where to store the properties.
     *   This method never returns {@code null}.
     *
     * @see org.netbeans.gradle.project.api.config.ProjectSettingsProvider#getExtensionSettings(String) ProjectSettingsProvider.getExtensionSettings
     */
    @Nonnull
    public String getExtensionName() {
        return extensionName;
    }

    /**
     * Returns the definition of the settings page.
     *
     * @return the definition of the settings page. This method never returns {@code null}.
     */
    @Nonnull
    public ProfileBasedSettingsCategory getSettingsCategory() {
        return settingsCetegory;
    }
}
