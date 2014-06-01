package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties2.standard.GradleLocationProperty;
import org.openide.modules.SpecificationVersion;

public abstract class AbstractProjectProperties implements ProjectProperties {
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
        return GradleLocationProperty.getGradleLocationFromString(gradleLocation);
    }

    public static String gradleLocationToString(GradleLocation gradleLocation) {
        return GradleLocationProperty.gradleLocationToString(gradleLocation);
    }

    @Override
    public Collection<OldMutableProperty<?>> getAllProperties() {
        List<OldMutableProperty<?>> result = new LinkedList<>();
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
