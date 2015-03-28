package org.netbeans.gradle.project.properties.global;

public interface GlobalSettingsEditor {
    public void updateSettings(GlobalGradleSettings globalSettings);
    public void saveSettings(GlobalGradleSettings globalSettings);

    public SettingsEditorProperties getProperties();
}
