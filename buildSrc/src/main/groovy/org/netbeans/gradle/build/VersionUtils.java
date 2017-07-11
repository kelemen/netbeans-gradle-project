package org.netbeans.gradle.build;

import org.gradle.api.Project;

public final class VersionUtils {
    public static String getTagForVersion(String version) {
        return 'v' + version;
    }

    public static String getReleaseVersion(Project project) {
        String defaultVersion = project.getVersion().toString();
        return PropertyUtils.getStringProperty(project, "releaseVersion", defaultVersion);
    }

    private VersionUtils() {
        throw new AssertionError();
    }
}
