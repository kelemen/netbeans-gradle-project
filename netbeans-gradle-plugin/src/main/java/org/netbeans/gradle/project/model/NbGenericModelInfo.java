package org.netbeans.gradle.project.model;

import java.io.File;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

// TODO: Make this class serializable (consider derived properties).
public final class NbGenericModelInfo {
    private final NbGradleMultiProjectDef projectDef;
    private final File settingsFile;

    // Derived properties
    private final String displayName;
    private final String description;

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef) {
        this(projectDef, findSettingsGradle(projectDef.getProjectDir()));
    }

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef, File settingsFile) {
        if (projectDef == null) throw new NullPointerException("projectTree");

        this.settingsFile = settingsFile;
        this.projectDef = projectDef;

        this.displayName = findDisplayName();
        this.description = findDescription();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public File getProjectDir() {
        return projectDef.getProjectDir();
    }

    public boolean isBuildSrc() {
        return getProjectDir().getName().equalsIgnoreCase(GradleProjectConstants.BUILD_SRC_NAME);
    }

    public File getBuildFile() {
        return projectDef.getMainProject().getGenericProperties().getBuildScript();
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public NbGradleMultiProjectDef getProjectDef() {
        return projectDef;
    }

    public NbGradleProjectTree getMainProject() {
        return projectDef.getMainProject();
    }

    public boolean isRootProject() {
        String uniqueName = getMainProject().getProjectFullName();
        for (int i = 0; i < uniqueName.length(); i++) {
            if (uniqueName.charAt(i) != ':') {
                return false;
            }
        }
        return true;
    }

    public FileObject tryGetProjectDirAsObj() {
        return FileUtil.toFileObject(getProjectDir());
    }

    public FileObject tryGetBuildFileObj() {
        return GradleFileUtils.asFileObject(getBuildFile());
    }

    public FileObject tryGetSettingsFileObj() {
        return GradleFileUtils.asFileObject(settingsFile);
    }

    public File getRootProjectDir() {
        File result = null;
        if (settingsFile != null) {
            result = settingsFile.getParentFile();
        }

        if (result == null) {
            result = getProjectDir();
        }
        return result;
    }

    public static File getBuildFile(File projectDir) {
        File buildFile = new File(projectDir, GradleProjectConstants.BUILD_FILE_NAME);
        if (buildFile.isFile()) {
            return buildFile;
        }

        buildFile = new File(projectDir, projectDir.getName() + GradleProjectConstants.DEFAULT_GRADLE_EXTENSION);
        if (buildFile.isFile()) {
            return buildFile;
        }

        return null;
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

    private String findDisplayName() {
        if (isBuildSrc()) {
            File parentFile = getProjectDir().getParentFile();
            String parentName = parentFile != null ? parentFile.getName() : "?";
            return NbStrings.getBuildSrcMarker(parentName);
        }
        else {
            String scriptName = getMainProject().getProjectName();
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

    private String findDescription() {
        if (isBuildSrc()) {
          // TODO(radimk) need some better description
          return findDisplayName();
        }
        else {
            String scriptName = getMainProject().getProjectFullName();
            scriptName = scriptName.trim();
            if (scriptName.isEmpty()) {
                scriptName = getProjectDir().getName();
            }

            String path = getMainProject().getProjectDir().getAbsolutePath();
            if (isRootProject()) {
                return NbStrings.getRootProjectDescription(scriptName, path);
            }
            else {
                return NbStrings.getSubProjectDescription(scriptName, path);
            }
        }
    }
}
