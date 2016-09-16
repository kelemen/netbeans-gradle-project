package org.netbeans.gradle.project.api.config.ui;

/**
 * Defines an immutable snapshot of some settings entries. These settings are always
 * associated with a {@link ProfileEditor}.
 *
 * @see ProfileEditor
 */
public interface StoredSettings {
    /**
     * Defines an implementation of {@code StoredSettings} whose methods do nothing but
     * throw an {@code IllegalStateException}.
     */
    public static final StoredSettings NOT_INITIALIZED = new StoredSettings() {
        @Override
        public void displaySettings() {
            throw new IllegalStateException("Settings has never been read.");
        }

        @Override
        public void saveSettings() {
            throw new IllegalStateException("Settings has never been read.");
        }
    };

    /**
     * Displays these settings on the associated UI component.
     * <P>
     * This method is always called on the <I>Event Dispatch Thread</I>.
     */
    public void displaySettings();

    /**
     * Persists these settings into the associated profile.
     * <P>
     * This method might be called on any thread.
     */
    public void saveSettings();
}
