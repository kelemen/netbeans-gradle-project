package org.netbeans.gradle.project.model;

import java.io.File;
import org.gradle.tooling.model.GradleProject;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

public final class NbGradleModel {
    private final File projectDir;
    private final File buildFile;
    private final File settingsFile;
    private final GradleProjectInfo projectInfo;
    private volatile boolean dirty;

    private final Lookup models;

    private final String displayName;

    public NbGradleModel(
            GradleProjectInfo projectInfo,
            File projectDir,
            Lookup models) {
        this(projectInfo, projectDir, findSettingsGradle(projectDir), models);
    }

    public NbGradleModel(
            GradleProjectInfo projectInfo,
            File projectDir,
            File settingsFile,
            Lookup models) {
        this(projectInfo, projectDir, getBuildFile(projectDir), settingsFile, models);
    }

    public NbGradleModel(
            GradleProjectInfo projectInfo,
            File projectDir,
            File buildFile,
            File settingsFile,
            Lookup models) {
        if (projectInfo == null) throw new NullPointerException("projectInfo");
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (models == null) throw new NullPointerException("models");

        this.projectInfo = projectInfo;
        this.projectDir = projectDir;
        this.buildFile = buildFile;
        this.settingsFile = settingsFile;
        this.models = models;
        this.dirty = false;
        this.displayName = findDisplayName();
    }

    private String findDisplayName() {
        if (isBuildSrc()) {
            File parentFile = getProjectDir().getParentFile();
            String parentName = parentFile != null ? parentFile.getName() : "?";
            return NbStrings.getBuildSrcMarker(parentName);
        }
        else {
            String scriptName = getGradleProject().getName();
            scriptName = scriptName.trim();
            if (scriptName.isEmpty()) {
                scriptName = getProjectDir().getName();
            }

            if (isRootProject()) {
                return NbStrings.getRootProjectMarker(scriptName);
            }
            else {
                return scriptName;
            }
        }
    }

    public static File getBuildFile(File projectDir) {
        // TODO: Check build file on the disk.
        // try build.gradle first, then directory.gradle.
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

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBuildSrc() {
        return projectDir.getName().equalsIgnoreCase(GradleProjectConstants.BUILD_SRC_NAME);
    }

    public boolean isRootProject() {
        String uniqueName = getGradleProject().getPath();
        for (int i = 0; i < uniqueName.length(); i++) {
            if (uniqueName.charAt(i) != ':') {
                return false;
            }
        }
        return true;
    }

    public GradleProjectInfo getGradleProjectInfo() {
        return projectInfo;
    }

    public GradleProject getGradleProject() {
        return projectInfo.getGradleProject();
    }

    public void setDirty() {
        this.dirty = true;
    }

    public NbGradleModel createNonDirtyCopy() {
        return new NbGradleModel(projectInfo, projectDir, buildFile, settingsFile, models);
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

    public File getBuildFile() {
        return buildFile;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public boolean isDirty() {
        return dirty;
    }

    public Lookup getModels() {
        return models;
    }

    public FileObject tryGetProjectDirAsObj() {
        return FileUtil.toFileObject(projectDir);
    }

    public FileObject tryGetBuildFileObj() {
        return GradleFileUtils.asFileObject(buildFile);
    }

    public FileObject tryGetSettingsFileObj() {
        return GradleFileUtils.asFileObject(settingsFile);
    }
}
