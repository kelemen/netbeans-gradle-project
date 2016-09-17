package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;

/**
 * Defines the logic of editing and saving properties of an associated profile.
 *
 * @see ProfileBasedSettingsCategory
 * @see ProfileEditorFactory
 */
public interface ProfileEditor {
    /**
     * Reads the settings from the associated profile and stores them immutably in the
     * returned {@code StoredSettings}.
     * <P>
     * This method can be called from any thread.
     *
     * @return the settings read from the associated profile. This method never returns {@code null}.
     */
    @Nonnull
    public StoredSettings readFromSettings();

    /**
     * Reads the settings from the associated UI component and stores them immutably in the
     * returned {@code StoredSettings}.
     * <P>
     * <P>
     * This method is always called on the <I>Event Dispatch Thread</I>.
     *
     * @return the settings read from the associated UI component. This method never returns {@code null}.
     */
    @Nonnull
    public StoredSettings readFromGui();
}
