package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.global.DebugMode;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

public class JavaProjectProperties {
    private final PropertyReference<DebugMode> debugMode;

    public JavaProjectProperties(ActiveSettingsQuery activeSettingsQuery) {
        this.debugMode = debugMode(activeSettingsQuery);
    }

    public PropertyReference<DebugMode> debugMode() {
        return debugMode;
    }

    public static PropertyReference<DebugMode> debugMode(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                DebugModeProjectProperty.PROPERTY_DEF,
                activeSettingsQuery,
                GlobalGradleSettings.getDefault().debugMode());
    }
}
