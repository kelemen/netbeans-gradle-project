package org.netbeans.gradle.project.model;

import java.io.File;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 * Defines an immutable model of a loaded Gradle project except that it can be
 * marked dirty. If the model is dirty, it means that the model should be
 * reloaded.
 */
public final class NbGradleModel {
    private final File projectDir;
    private final File buildFile;
    private final File settingsFile;
    private volatile boolean dirty;

    private final NbGradleModule mainModule;
    private final Lookup models;

    public NbGradleModel(
            File projectDir,
            NbGradleModule mainModule,
            Lookup models) {
        this(projectDir, findSettingsGradle(projectDir), mainModule, models);
    }

    public NbGradleModel(
            File projectDir,
            File settingsFile,
            NbGradleModule mainModule,
            Lookup models) {
        this(projectDir, getBuildFile(projectDir), settingsFile, mainModule, models);
    }

    public NbGradleModel(
            File projectDir,
            File buildFile,
            File settingsFile,
            NbGradleModule mainModule,
            Lookup models) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (mainModule == null) throw new NullPointerException("mainModule");
        if (models == null) throw new NullPointerException("models");

        this.projectDir = projectDir;
        this.mainModule = mainModule;
        this.models = models;

        this.buildFile = buildFile;
        this.settingsFile = settingsFile;
        this.dirty = false;
    }

    public static File getBuildFile(File projectDir) {
        return new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME);
    }

    public static FileObject getBuildFile(FileObject projectDir) {
        return projectDir.getFileObject(GradleProjectConstants.BUILD_FILE_NAME);
    }

    public static File findSettingsGradle(File projectDir) {
        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        FileObject resultObj = findSettingsGradle(projectDirObj);
        return resultObj != null
                ? FileUtil.toFile(resultObj)
                : null;
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

    public Lookup getModels() {
        return models;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public NbGradleModel createNonDirtyCopy() {
        return new NbGradleModel(projectDir, buildFile, settingsFile, mainModule, models);
    }

    public File getProjectDir() {
        return projectDir;
    }

    public File getRootProjectDir() {
        File result = null;
        if (settingsFile != null) {
            result = settingsFile.getParentFile();
        }

        if (result == null) {
            result = projectDir;
        }
        return result;
    }

    public FileObject tryGetProjectDirAsObj() {
        return FileUtil.toFileObject(projectDir);
    }


    public NbGradleModule getMainModule() {
        return mainModule;
    }

    public File getBuildFile() {
        return buildFile;
    }

    public FileObject tryGetBuildFileObj() {
        return GradleFileUtils.asFileObject(buildFile);
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public FileObject tryGetSettingsFileObj() {
        return GradleFileUtils.asFileObject(settingsFile);
    }
}
