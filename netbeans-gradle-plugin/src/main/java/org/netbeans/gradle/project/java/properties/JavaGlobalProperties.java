package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.GlobalConfig;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.java.JavaExtensionDef;

public class JavaGlobalProperties {
    private final PropertyReference<DebugMode> debugMode;

    public JavaGlobalProperties(ActiveSettingsQuery activeSettingsQuery) {
        this.debugMode = JavaProjectProperties.debugMode(activeSettingsQuery);
    }

    public PropertyReference<DebugMode> debugMode() {
        return debugMode;
    }

    public static JavaGlobalProperties getDefault() {
        return DefaultHolder.DEFAULT;
    }

    private static class DefaultHolder {
        private static final JavaGlobalProperties DEFAULT = new JavaGlobalProperties(
                GlobalConfig.getGlobalSettingsQuery(JavaExtensionDef.EXTENSION_NAME));
    }
}
