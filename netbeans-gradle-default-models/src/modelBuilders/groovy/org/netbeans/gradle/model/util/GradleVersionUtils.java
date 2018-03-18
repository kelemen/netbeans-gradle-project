package org.netbeans.gradle.model.util;

import org.gradle.util.GradleVersion;

public final class GradleVersionUtils {
    public static final boolean GRADLE_4_OR_BETTER = isBetterOrEqual("4.0");

    private static boolean isBetterOrEqual(String version) {
        return GradleVersion.current().compareTo(GradleVersion.version(version)) >= 0;
    }

    private GradleVersionUtils() {
        throw new AssertionError();
    }
}
