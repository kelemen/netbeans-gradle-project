package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;

public interface ProfileValuesEditorFactory {
    public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery);
}
