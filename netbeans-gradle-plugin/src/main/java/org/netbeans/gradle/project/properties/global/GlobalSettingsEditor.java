package org.netbeans.gradle.project.properties.global;

import org.netbeans.gradle.project.api.config.ui.ProfileEditorFactory;

public interface GlobalSettingsEditor extends ProfileEditorFactory {
    public SettingsEditorProperties getProperties();
}
