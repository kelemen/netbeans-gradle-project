package org.netbeans.gradle.project.properties.global;

import org.netbeans.gradle.project.properties.ProfileEditorFactory;

public interface GlobalSettingsEditor extends ProfileEditorFactory {
    public SettingsEditorProperties getProperties();
}
