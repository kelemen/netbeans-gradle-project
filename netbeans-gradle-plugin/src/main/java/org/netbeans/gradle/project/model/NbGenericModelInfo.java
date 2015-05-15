package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class NbGenericModelInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NbGradleMultiProjectDef projectDef;
    private final Path settingsFile;

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef) {
        this(projectDef, findSettingsGradle(projectDef.getProjectDir()));
    }

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef, Path settingsFile) {
        ExceptionHelper.checkNotNullArgument(projectDef, "projectDef");

        this.settingsFile = settingsFile;
        this.projectDef = projectDef;
    }

    public File getProjectDir() {
        return projectDef.getProjectDir();
    }

    public boolean isBuildSrc() {
        return getProjectDir().getName().equalsIgnoreCase(SettingsFiles.BUILD_SRC_NAME);
    }

    public File getBuildDir() {
        return projectDef.getMainProject().getGenericProperties().getBuildDir();
    }

    public File getBuildFile() {
        return projectDef.getMainProject().getGenericProperties().getBuildScript();
    }

    private File getSettingsFileAsFile() {
        return settingsFile != null ? settingsFile.toFile() : null;
    }

    public Path getSettingsFile() {
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
        return NbFileUtils.asFileObject(getBuildFile());
    }

    public FileObject tryGetSettingsFileObj() {
        return NbFileUtils.asFileObject(getSettingsFileAsFile());
    }

    /**
     * Returns the directory containing the {@code settings.gradle} file.
     * This method also works for the {@code buildSrc} project, for which this
     * returns the directory of the root project this {@code buildSrc} project
     * belongs to.
     *
     * @return the directory containing the {@code settings.gradle} file.
     *   This method never returns {@code null}.
     */
    public Path getSettingsDir() {
        Path result = null;
        if (settingsFile != null) {
            result = settingsFile.getParent();
        }

        if (result == null) {
            result = getProjectDir().toPath();
        }
        return result;
    }

    public static File getBuildFile(File projectDir) {
        File buildFile = new File(projectDir, SettingsFiles.BUILD_FILE_NAME);
        if (buildFile.isFile()) {
            return buildFile;
        }

        buildFile = new File(projectDir, projectDir.getName() + SettingsFiles.DEFAULT_GRADLE_EXTENSION);
        if (buildFile.isFile()) {
            return buildFile;
        }

        return null;
    }

    private static File findSettingsGradleAsFile(File projectDir) {
        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        FileObject resultObj = findSettingsGradle(projectDirObj);
        return resultObj != null
                ? FileUtil.toFile(resultObj)
                : null;
    }

    public static Path findSettingsGradle(File projectDir) {
        File result = findSettingsGradleAsFile(projectDir);
        return result != null ? result.toPath() : null;
    }

    public static FileObject findSettingsGradle(FileObject projectDir) {
        if (projectDir == null) {
            return null;
        }

        FileObject settingsGradle = projectDir.getFileObject(SettingsFiles.SETTINGS_GRADLE);
        if (settingsGradle != null && !settingsGradle.isVirtual()) {
            return settingsGradle;
        }
        else {
            return findSettingsGradle(projectDir.getParent());
        }
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final NbGradleMultiProjectDef projectDef;
        private final File settingsFile; // for backward compatibility
        private final String settingsPath;

        public SerializedFormat(NbGenericModelInfo source) {
            this.projectDef = source.projectDef;
            this.settingsFile = null;
            this.settingsPath = source.settingsFile != null
                    ? source.settingsFile.toString()
                    : null;
        }

        public Path getSettingsPath() {
            if (settingsPath != null) {
                return Paths.get(settingsPath);
            }

            return settingsFile.toPath();
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbGenericModelInfo(projectDef, getSettingsPath());
        }
    }
}
