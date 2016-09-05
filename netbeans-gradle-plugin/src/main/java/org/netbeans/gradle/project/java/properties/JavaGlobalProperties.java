package org.netbeans.gradle.project.java.properties;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.GlobalSettingsProvider;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.java.JavaExtensionDef;
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
            GlobalSettingsProvider globalSettings = Lookup.getDefault().lookup(GlobalSettingsProvider.class);
            DEFAULT = globalSettings != null
                    ? new JavaGlobalProperties(globalSettings.getExtensionSettings(JavaExtensionDef.EXTENSION_NAME))
                    : null;
        }
    }
}
