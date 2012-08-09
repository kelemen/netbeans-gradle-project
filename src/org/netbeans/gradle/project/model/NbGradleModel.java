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
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (mainModule == null) throw new NullPointerException("mainModule");

        this.projectDir = projectDir;
        this.mainModule = mainModule;

        this.buildFile = projectDir.getFileObject("build.gradle");
        if (this.buildFile == null) {
            throw new IOException("Missing build.gradle in the project directory: " + projectDir);
        }
        this.settingsFile = findSettingsGradle(projectDir);
    }

    private static FileObject findSettingsGradle(FileObject rootDir) {
        if (rootDir == null) {
            return null;
        }

        FileObject settingsGradle = rootDir.getFileObject("settings.gradle");
        if (settingsGradle != null && !settingsGradle.isVirtual()) {
            return settingsGradle;
        }
        else {
            return findSettingsGradle(rootDir.getParent());
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
