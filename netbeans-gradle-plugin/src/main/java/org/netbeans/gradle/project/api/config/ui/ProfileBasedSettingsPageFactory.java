package org.netbeans.gradle.project.api.config.ui;

/**
 * Defines a factory to create the component and to display the editor of the
 * properties and the logic of setting the properties in the settings.
 *
 * @see ProfileBasedSettingsCategory
 */
public interface ProfileBasedSettingsPageFactory {
    /**
     * Creates a new properties category page which is able to adjust
     * configuration of specific profiles.
     * <P>
     * This method is always called on the <I>Event Dispatch Thread</I>.
     *
     * @return a new properties category page which is able to adjust
     *   configuration of specific profiles. This method may never return
     *   {@code null}.
     */
    public ProfileBasedSettingsPage createSettingsPage();
}
