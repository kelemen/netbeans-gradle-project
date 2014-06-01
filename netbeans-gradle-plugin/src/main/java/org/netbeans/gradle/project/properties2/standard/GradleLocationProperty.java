package org.netbeans.gradle.project.properties2.standard;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.GradleLocationDirectory;
import org.netbeans.gradle.project.properties.GradleLocationDistribution;
import org.netbeans.gradle.project.properties.GradleLocationVersion;

public final class GradleLocationProperty {
    private static final Logger LOGGER = Logger.getLogger(GradleLocationProperty.class.getName());

    public static GradleLocation getGradleLocationFromString(String gradleLocation) {
        ExceptionHelper.checkNotNullArgument(gradleLocation, "gradleLocation");

        String location = gradleLocation.trim();
        if (location.isEmpty()) {
            return GradleLocationDefault.INSTANCE;
        }

        if (location.startsWith("?")) {
            int sepIndex = location.indexOf('=');
            if (sepIndex >= 0) {
                String typeName = location.substring(1, sepIndex).trim();
                String value = location.substring(sepIndex + 1, location.length()).trim();

                if (GradleLocationDefault.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
                    return GradleLocationDefault.INSTANCE;
                }
                if (GradleLocationVersion.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
                    return new GradleLocationVersion(value);
                }
                if (GradleLocationDirectory.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
                    return new GradleLocationDirectory(new File(value));
                }
                if (GradleLocationDistribution.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
                    try {
                        return new GradleLocationDistribution(new URI(value));
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.INFO, "Invalid URI for Gradle distribution: " + value, ex);
                    }
                }
            }
        }

        return new GradleLocationDirectory(new File(location));
    }

    private GradleLocationProperty() {
        throw new AssertionError();
    }
}
