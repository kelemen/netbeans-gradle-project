package org.netbeans.gradle.project.model;

import java.io.IOException;
import org.openide.filesystems.FileObject;

/**
 * Defines an immutable model of a loaded Gradle project.
 * The properties of this model which are not truly immutable are the
 * {@code FileObject} defining the project directory, "build.gradle" and the
 * "settings.gradle" files.
 */
public final class NbGradleModel {
    private final FileObject projectDir;
    private final FileObject buildFile;
    private final FileObject settingsFile;

    private final NbGradleModule mainModule;

    public NbGradleModel(
            FileObject projectDir,
            NbGradleModule mainModule) throws IOException {
        this(projectDir, findSettingsGradle(projectDir), mainModule);
    }

    public NbGradleModel(
            FileObject projectDir,
            FileObject settingsFile,
            NbGradleModule mainModule) throws IOException {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (mainModule == null) throw new NullPointerException("mainModule");

        this.projectDir = projectDir;
        this.mainModule = mainModule;

        this.buildFile = getBuildFile(projectDir);
        this.settingsFile = settingsFile;
    }

    public static FileObject getBuildFile(FileObject projectDir) throws IOException {
        FileObject buildFile = projectDir.getFileObject("build.gradle");
        if (buildFile == null) {
            throw new IOException("Missing build.gradle in the project directory: " + projectDir);
        }
        return buildFile;
    }

    public static FileObject findSettingsGradle(FileObject projectDir) {
        if (projectDir == null) {
            return null;
        }

        FileObject settingsGradle = projectDir.getFileObject("settings.gradle");
        if (settingsGradle != null && !settingsGradle.isVirtual()) {
            return settingsGradle;
        }
        else {
            return findSettingsGradle(projectDir.getParent());
        }
    }

    public FileObject getProjectDir() {
        return projectDir;
    }

    public NbGradleModule getMainModule() {
        return mainModule;
    }

    public FileObject getBuildFile() {
        return buildFile;
    }

    public FileObject getSettingsFile() {
        return settingsFile;
    }

}
