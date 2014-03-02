package org.netbeans.gradle.project.properties;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.openide.modules.SpecificationVersion;

public abstract class AbstractProjectProperties implements ProjectProperties {
    private static final Logger LOGGER = Logger.getLogger(AbstractProjectProperties.class.getName());

    public static final String DEFAULT_SOURCE_LEVEL = "1.7";
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    private static String getSourceLevelFromPlatformVersion(String version) {
        String[] versionParts = version.split(Pattern.quote("."));
        if (versionParts.length < 2) {
            return DEFAULT_SOURCE_LEVEL;
        }
        else {
            return versionParts[0] + "." + versionParts[1];
        }
    }

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        SpecificationVersion version = platform.getSpecification().getVersion();
        String versionStr = version != null ? version.toString() : "";
        return getSourceLevelFromPlatformVersion(versionStr);
    }

    public static String getSourceLevelFromPlatform(ProjectPlatform platform) {
        return getSourceLevelFromPlatformVersion(platform.getVersion());
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

    public static String gradleLocationToString(GradleLocation gradleLocation) {
        String value = gradleLocation.asString();
        if (value == null) {
            return "";
        }

        String typeName = gradleLocation.getUniqueTypeName();
        return "?" + typeName + "=" + value;
    }

    @Override
    public Collection<MutableProperty<?>> getAllProperties() {
        List<MutableProperty<?>> result = new LinkedList<>();
        result.add(getPlatform());
        result.add(getSourceEncoding());
        result.add(getSourceLevel());
        result.add(getCommonTasks());
        result.add(getScriptPlatform());
        result.add(getGradleLocation());
        result.add(getLicenseHeader());
        result.add(getAuxConfigListener());
        return result;
    }
}
