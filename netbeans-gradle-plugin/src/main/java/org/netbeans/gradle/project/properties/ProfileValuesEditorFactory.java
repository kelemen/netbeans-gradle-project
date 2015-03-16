package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.properties2.ActiveSettingsQuery;

public interface ProfileValuesEditorFactory {
    public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery);
}
