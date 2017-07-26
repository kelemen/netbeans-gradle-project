package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class NbGenericModelInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final NbGradleMultiProjectDef projectDef;
    private final Path settingsFile;
    private final long createTimeEpochMs;

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef, Path settingsFile) {
        this(projectDef, settingsFile, System.currentTimeMillis());
    }

    public NbGenericModelInfo(NbGradleMultiProjectDef projectDef, Path settingsFile, long createTimeEpochMs) {
        ExceptionHelper.checkNotNullArgument(projectDef, "projectDef");

        this.settingsFile = settingsFile;
        this.projectDef = projectDef;
        this.createTimeEpochMs = createTimeEpochMs;
    }

    public long getCreateTimeEpochMs() {
        return createTimeEpochMs;
    }

    public File getProjectDir() {
        return projectDef.getProjectDir();
    }

    public boolean isBuildSrc() {
        return getProjectDir().getName().equalsIgnoreCase(CommonScripts.BUILD_SRC_NAME);
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

    public static FileObject findSettingsGradle(FileObject projectDir, ScriptFileProvider scriptProvider) {
        if (projectDir == null) {
            return null;
        }

        File projectDirFile = FileUtil.toFile(projectDir);
        if (projectDirFile == null) {
            return null;
        }

        Path resultPath = ModelLoadUtils.findSettingsGradle(projectDirFile.toPath(), scriptProvider);
        if (resultPath == null) {
            return null;
        }

        return FileUtil.toFileObject(resultPath.toFile());
    }

    public static Path tryGuessBuildFilePath(Path projectDir, ScriptFileProvider scriptProvider) {
        Path result = scriptProvider.findScriptFile(projectDir, CommonScripts.BUILD_BASE_NAME);
        if (result != null) {
            return result;
        }

        result = scriptProvider.findScriptFile(projectDir, CommonScripts.SETTINGS_BASE_NAME);
        if (result != null) {
            return result;
        }

        String baseName = NbFileUtils.getFileNameStr(projectDir);
        result = scriptProvider.findScriptFile(projectDir, baseName);
        if (result != null) {
            return result;
        }

        return scriptProvider.findScriptFile(projectDir, baseName + "-" + CommonScripts.BUILD_BASE_NAME);
    }

    public static GenericProjectProperties createProjectProperties(
            String projectName,
            String projectFullName,
            Path projectDir,
            ScriptFileProvider scriptProvider) {

        Path buildFile = tryGuessBuildFilePath(projectDir, scriptProvider);
        if (buildFile == null) {
            buildFile = projectDir.resolve(CommonScripts.BUILD_BASE_NAME + CommonScripts.DEFAULT_SCRIPT_EXTENSION);
        }

        return new GenericProjectProperties(projectName, projectFullName, projectDir.toFile(), buildFile.toFile());
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
        private final Long createTimeEpochMs;

        public SerializedFormat(NbGenericModelInfo source) {
            this.projectDef = source.projectDef;
            this.settingsFile = null;
            this.settingsPath = source.settingsFile != null
                    ? source.settingsFile.toString()
                    : null;
            this.createTimeEpochMs = source.createTimeEpochMs;
        }

        public Path getSettingsPath() {
            if (settingsPath != null) {
                return Paths.get(settingsPath);
            }

            return settingsFile != null ? settingsFile.toPath() : null;
        }

        public long getCreateTimeEpochMs() {
            return createTimeEpochMs != null ? createTimeEpochMs : System.currentTimeMillis();
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbGenericModelInfo(projectDef, getSettingsPath(), getCreateTimeEpochMs());
        }
    }
}
