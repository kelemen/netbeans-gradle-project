package org.netbeans.gradle.project.properties.global;

public interface GlobalSettingsEditor {
    public void updateSettings();
    public void saveSettings();

    public SettingsEditorProperties getProperties();
}
