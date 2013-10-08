package org.netbeans.gradle.project.java.model;

public enum JavaModelSource {
    COMPATIBLE_API(false),
    GRADLE_1_8_API(true);

    private final boolean reliableJavaVersion;

    private JavaModelSource(boolean reliableJavaVersion) {
        this.reliableJavaVersion = reliableJavaVersion;
    }

    public boolean isReliableJavaVersion() {
        return reliableJavaVersion;
    }
}
