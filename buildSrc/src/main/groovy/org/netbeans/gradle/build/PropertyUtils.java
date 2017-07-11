package org.netbeans.gradle.build;

import java.util.Locale;
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

    public static boolean getBoolProperty(Project project, String name, boolean defaultValue) {
        String strValue = getStringProperty(project, name, null);
        if (strValue == null) {
            return defaultValue;
        }

        String normValue = strValue.toLowerCase(Locale.ROOT).trim();
        if ("true".equals(normValue)) {
            return true;
        }
        if ("false".equals(normValue)) {
            return false;
        }
        return defaultValue;
    }

    private PropertyUtils() {
        throw new AssertionError();
    }
}
