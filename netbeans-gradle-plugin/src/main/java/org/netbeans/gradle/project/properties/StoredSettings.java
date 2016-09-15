package org.netbeans.gradle.project.properties;

public interface StoredSettings {
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

    public void displaySettings();
    public void saveSettings();
}
