package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties2.standard.GradleLocationProperty;
import org.netbeans.gradle.project.properties2.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.properties2.standard.SourceLevelProperty;

public abstract class AbstractProjectProperties implements ProjectProperties {
    public static final String DEFAULT_SOURCE_LEVEL = SourceLevelProperty.DEFAULT_SOURCE_LEVEL;
    public static final Charset DEFAULT_SOURCE_ENCODING = SourceEncodingProperty.DEFAULT_SOURCE_ENCODING;

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        return SourceLevelProperty.getSourceLevelFromPlatform(platform);
    }

    public static String getSourceLevelFromPlatform(ProjectPlatform platform) {
        return SourceLevelProperty.getSourceLevelFromPlatform(platform);
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
