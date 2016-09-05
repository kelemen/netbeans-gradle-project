package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.GradleGlobalSettingsProvider;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.java.JavaExtensionDef;
import org.netbeans.gradle.project.properties.global.DebugMode;
import org.openide.util.Lookup;

public class JavaGlobalProperties {
    private final PropertyReference<DebugMode> debugMode;

    public JavaGlobalProperties(ActiveSettingsQuery activeSettingsQuery) {
        this.debugMode = JavaProjectProperties.debugMode(activeSettingsQuery);
    }

    public PropertyReference<DebugMode> debugMode() {
        return debugMode;
    }

    public static JavaGlobalProperties getDefault() {
        if (DefaultHolder.DEFAULT == null) {
            throw new IllegalStateException("Failed to load default settings.");
        }
        return DefaultHolder.DEFAULT;
    }

    private static class DefaultHolder {
        private static final JavaGlobalProperties DEFAULT;

        static {
            GradleGlobalSettingsProvider globalSettings = Lookup.getDefault().lookup(GradleGlobalSettingsProvider.class);
            DEFAULT = globalSettings != null
                    ? new JavaGlobalProperties(globalSettings.getExtensionSettings(JavaExtensionDef.EXTENSION_NAME))
                    : null;
        }
    }
}
