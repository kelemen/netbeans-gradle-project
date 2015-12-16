package org.netbeans.gradle.project.api.config.ui;

import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;

/**
 * Defines the logic of editing multiple profiles on the associated settings
 * panel.
 *
 * @see ProfileBasedConfigurations
 * @see ProfileBasedProjectSettingsPageFactory
 */
public interface ProfileValuesEditorFactory {
    /**
     * Prepares editing the selected profile. This method is called lazily
     * when the user first selects a profile to edit. Note that this method
     * should not update UI, the UI should only be update when the
     * {@link ProfileValuesEditor#displayValues() displayValues} method of the
     * returned {@code ProfileValuesEditor} gets called.
     * <P>
     * To retrieve the key of the profile to be edited, you can use the following
     * call: {@code profileQuery.currentProfileSettings().getValue().getKey()}.
     * <P>
     * <B>Note</B>: This method can be called from any thread.
     *
     * @param displayName the display name of the profile to be edited. This
     *   argument cannot be {@code null}.
     * @param profileQuery the {@code ActiveSettingsQuery} with the profile
     *   to be edited as a {@link ActiveSettingsQuery#currentProfileSettings() selected profile}.
     *   The {@code currentProfileSettings} property of this {@code ActiveSettingsQuery}
     *   will never change. This argument cannot be {@code null}.
     * @return the logic of editing and saving the properties in the selected
     *   profile. This method may never return {@code null}.
     */
    @Nonnull
    public ProfileValuesEditor startEditingProfile(@Nonnull String displayName, @Nonnull ActiveSettingsQuery profileQuery);
}
