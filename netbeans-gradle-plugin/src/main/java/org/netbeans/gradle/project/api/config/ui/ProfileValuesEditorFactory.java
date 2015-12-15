package org.netbeans.gradle.project.api.config.ui;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;

public interface ProfileValuesEditorFactory {
    public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery);
}
