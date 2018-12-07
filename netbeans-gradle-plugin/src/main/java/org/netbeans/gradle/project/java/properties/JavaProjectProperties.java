package org.netbeans.gradle.project.java.properties;

import org.jtrim2.property.PropertyFactory;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.properties.standard.CommonProperties;

import static org.netbeans.gradle.project.properties.standard.CommonProperties.*;

public class JavaProjectProperties {
    private final PropertyReference<DebugMode> debugMode;
    private final PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayMode;
    private final PropertyReference<Boolean> allowModules;

    public JavaProjectProperties(ActiveSettingsQuery activeSettingsQuery) {
        this.debugMode = debugMode(activeSettingsQuery);
        this.javaSourcesDisplayMode = javaSourcesDisplayMode(activeSettingsQuery);
        this.allowModules = allowModules(activeSettingsQuery);
    }

    public PropertyReference<DebugMode> debugMode() {
        return debugMode;
    }

    public static PropertyReference<DebugMode> debugMode(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                DebugModeProjectProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(DebugModeProjectProperty.DEFAULT));
    }

    public PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayMode() {
        return javaSourcesDisplayMode;
    }

    public static PropertyReference<JavaSourcesDisplayMode> javaSourcesDisplayMode(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                defineEnumProperty(JavaSourcesDisplayMode.class, "appearance", "project-node", "display-mode"),
                activeSettingsQuery,
                PropertyFactory.constSource(JavaSourcesDisplayMode.DEFAULT_MODE));
    }

    public static PropertyReference<Boolean> allowModules(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                CommonProperties.defineBooleanProperty("modules", "allow"),
                activeSettingsQuery,
                PropertyFactory.constSource(true));
    }

    public PropertyReference<Boolean> allowModules() {
        return allowModules;
    }
}
