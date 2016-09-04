package org.netbeans.gradle.project.properties.global;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;

public interface GlobalSettingsEditor {
    public void updateSettings(ActiveSettingsQuery globalSettings);
    public void saveSettings(ActiveSettingsQuery globalSettings);

    public SettingsEditorProperties getProperties();
}
