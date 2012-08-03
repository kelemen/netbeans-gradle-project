package org.netbeans.gradle.project;

import org.netbeans.api.java.project.JavaProjectConstants;

public final class GradleProjectConstants {
    public static final String SOURCES = JavaProjectConstants.SOURCES_TYPE_JAVA;
    public static final String TEST_SOURCES = JavaProjectConstants.SOURCES_HINT_TEST;
    public static final String RESOURCES = JavaProjectConstants.SOURCES_TYPE_RESOURCES;
    public static final String TEST_RESOURCES = "test-resources";

    private GradleProjectConstants() {
        throw new AssertionError();
    }
}
