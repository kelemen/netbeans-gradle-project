package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;

/**
 * Defines the logic of editing and saving properties of an associated profile.
 *
 * @see ProfileBasedConfigurations
 * @see ProfileEditorFactory
 */
public interface ProfileEditor {
    @Nonnull
    public StoredSettings readFromSettings();

    @Nonnull
    public StoredSettings readFromGui();
}
