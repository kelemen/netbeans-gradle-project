package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.properties.PropertyReference;
import org.netbeans.gradle.project.properties.global.DebugMode;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

public class JavaProjectProperties {
    // TODO: The API should automatically add this part
    public static final ConfigPath PARENT_PATH = ConfigPath.fromKeys("extensions", "org.netbeans.gradle.project.java.JavaExtension");

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
