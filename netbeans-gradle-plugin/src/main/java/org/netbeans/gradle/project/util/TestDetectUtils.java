package org.netbeans.gradle.project.util;

public final class TestDetectUtils {
    public static boolean isRunningTests() {
        return "true".equalsIgnoreCase(System.getProperty("org.netbeans.gradle.runningTests"));
    }

    private TestDetectUtils() {
        throw new AssertionError();
    }
}
