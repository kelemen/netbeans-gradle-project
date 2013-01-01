package org.netbeans.gradle.project.model;

import org.netbeans.gradle.project.GradleProjectConstants;
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
    private volatile boolean dirty;

    private final NbGradleModule mainModule;

    public NbGradleModel(
            FileObject projectDir,
            NbGradleModule mainModule) {
        this(projectDir, findSettingsGradle(projectDir), mainModule);
    }

    public NbGradleModel(
            FileObject projectDir,
            FileObject settingsFile,
            NbGradleModule mainModule) {
        this(projectDir, getBuildFile(projectDir), settingsFile, mainModule);
    }

    public NbGradleModel(
            FileObject projectDir,
            FileObject buildFile,
            FileObject settingsFile,
            NbGradleModule mainModule) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (mainModule == null) throw new NullPointerException("mainModule");

        this.projectDir = projectDir;
        this.mainModule = mainModule;

        this.buildFile = buildFile;
        this.settingsFile = settingsFile;
        this.dirty = false;
    }

    public static FileObject getBuildFile(FileObject projectDir) {
        return projectDir.getFileObject(GradleProjectConstants.BUILD_FILE_NAME);
    }

    public static FileObject findSettingsGradle(FileObject projectDir) {
        if (projectDir == null) {
            return null;
        }

        FileObject settingsGradle = projectDir.getFileObject(GradleProjectConstants.SETTINGS_FILE_NAME);
        if (settingsGradle != null && !settingsGradle.isVirtual()) {
            return settingsGradle;
        }
        else {
            return findSettingsGradle(projectDir.getParent());
        }
    }

    public void setDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public NbGradleModel createNonDirtyCopy() {
        return new NbGradleModel(projectDir, buildFile, settingsFile, mainModule);
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
