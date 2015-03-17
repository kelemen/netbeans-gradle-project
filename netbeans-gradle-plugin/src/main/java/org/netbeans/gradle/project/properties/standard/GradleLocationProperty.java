package org.netbeans.gradle.project.properties.standard;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.GradleLocationDirectory;
import org.netbeans.gradle.project.properties.GradleLocationDistribution;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyValueDef;

public final class GradleLocationProperty {
    private static final Logger LOGGER = Logger.getLogger(GradleLocationProperty.class.getName());

    private static final String CONFIG_KEY_GRADLE_LOCATION = "gradle-home";

    public static final PropertyDef<String, GradleLocation> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, GradleLocation> createPropertyDef() {
        PropertyDef.Builder<String, GradleLocation> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_GRADLE_LOCATION));
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    private static GradleLocation tryGetGradleLocationFromString(String gradleLocation) {
        return gradleLocation != null
                ? getGradleLocationFromString(gradleLocation)
                : null;
    }

    public static String gradleLocationToString(GradleLocation gradleLocation) {
        String value = gradleLocation.asString();
        if (value == null) {
            return "";
        }

        String typeName = gradleLocation.getUniqueTypeName();
        return "?" + typeName + "=" + value;
    }

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

    private static PropertyValueDef<String, GradleLocation> getValueDef() {
        return new PropertyValueDef<String, GradleLocation>() {
            @Override
            public PropertySource<GradleLocation> property(String valueKey) {
                return PropertyFactory.constSource(tryGetGradleLocationFromString(valueKey));
            }

            @Override
            public String getKeyFromValue(GradleLocation value) {
                return value != null
                        ? gradleLocationToString(value)
                        : null;
            }
        };
    }

    private GradleLocationProperty() {
        throw new AssertionError();
    }
}
