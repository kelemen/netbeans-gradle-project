package org.netbeans.gradle.project.java.model;

import java.io.File;

public final class NbOutput {
    private final File buildDir;
    private final File testBuildDir;

    public NbOutput(File buildDir, File testBuildDir) {
        if (buildDir == null) throw new NullPointerException("buildDir");
        if (testBuildDir == null) throw new NullPointerException("testBuildDir");

        this.buildDir = buildDir;
        this.testBuildDir = testBuildDir;
    }

    public File getBuildDir() {
        return buildDir;
    }

    public File getTestBuildDir() {
        return testBuildDir;
    }
}
