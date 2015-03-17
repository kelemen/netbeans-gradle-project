package org.netbeans.gradle.project.properties;

public interface ProfileValuesEditorFactory {
    public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery);
}
