package org.netbeans.gradle.build;

import org.gradle.api.Project;

public final class PropertyUtils {
    public static String getStringProperty(Project project, String name, String defaultValue) {
        if (!project.hasProperty(name)) {
            return defaultValue;
        }

        Object propertyValue = project.property(name);
        String result = propertyValue != null ? propertyValue.toString() : null;
        return result != null ? result.trim() : defaultValue;
    }

    private PropertyUtils() {
        throw new AssertionError();
    }
}
