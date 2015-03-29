package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.gradle.project.view.DisplayableTaskVariable;
import org.openide.filesystems.FileObject;

public final class NbGradleModel {
    public static final class Builder {
        private final NbGenericModelInfo genericInfo;
        private final Map<String, Object> extensionModels;

        public Builder(NbGenericModelInfo genericInfo) {
            ExceptionHelper.checkNotNullArgument(genericInfo, "genericInfo");

            this.genericInfo = genericInfo;
            this.extensionModels = new HashMap<>();
        }

        public Builder(NbGradleModel base) {
            this.genericInfo = base.getGenericInfo();
            this.extensionModels = new HashMap<>(base.extensionModels);
        }

        public File getProjectDir() {
            return genericInfo.getProjectDir();
        }

        public void setModelForExtension(NbGradleExtensionRef extension, Object model) {
            setModelForExtension(extension.getName(), model);
        }

        public void setModelForExtension(String extensionName, Object model) {
            extensionModels.put(extensionName, model);
        }

        public NbGradleModel create() {
            return new NbGradleModel(genericInfo, extensionModels);
        }
    }

    private final NbGenericModelInfo genericInfo;

    // Maps extension name to extension model
    private final Map<String, Object> extensionModels;

    public NbGradleModel(NbGradleMultiProjectDef projectDef) {
        this(new NbGenericModelInfo(projectDef), Collections.<String, Object>emptyMap(), false);
    }

    public NbGradleModel(
            NbGenericModelInfo genericInfo,
            Map<String, Object> extensionModels) {
        this(genericInfo, extensionModels, true);
    }

    private NbGradleModel(
            NbGenericModelInfo genericInfo,
            Map<String, Object> extensionModels,
            boolean copyMap) {

        ExceptionHelper.checkNotNullArgument(genericInfo, "genericInfo");

        this.genericInfo = genericInfo;
        this.extensionModels = copyMap
                ? CollectionUtils.copyNullSafeHashMapWithNullValues(extensionModels)
                : extensionModels;
    }

    public static List<NbGradleModel> createAll(Collection<? extends Builder> builders) {
        List<NbGradleModel> result = new ArrayList<>(builders.size());
        for (Builder builder: builders) {
            result.add(builder.create());
        }
        return result;
    }

    public NbGenericModelInfo getGenericInfo() {
        return genericInfo;
    }

    public boolean hasModelOfExtension(NbGradleExtensionRef extension) {
        return hasModelOfExtension(extension.getName());
    }

    public boolean hasModelOfExtension(String extensionName) {
        return extensionModels.containsKey(extensionName);
    }

    public Object getModelOfExtension(NbGradleExtensionRef extension) {
        return extensionModels.get(extension.getName());
    }

    public Object getModelOfExtension(String extensionName) {
        return extensionModels.get(extensionName);
    }

    public Map<String, Object> getExtensionModels() {
        return extensionModels;
    }

    public void setModelForExtension(NbGradleExtensionRef extension) {
        extension.setModelForExtension(extensionModels.get(extension.getName()));
    }

    public static File getBuildFile(File projectDir) {
        return NbGenericModelInfo.getBuildFile(projectDir);
    }

    public static File findSettingsGradle(File projectDir) {
        return NbGenericModelInfo.findSettingsGradle(projectDir);
    }

    public static FileObject findSettingsGradle(FileObject projectDir) {
        return NbGenericModelInfo.findSettingsGradle(projectDir);
    }

    public ProjectId getProjectId() {
        return genericInfo.getMainProject().getGenericProperties().getProjectId();
    }

    public String getDisplayName(String namePattern) {
        String rawName = StandardTaskVariable.replaceVars(namePattern, DisplayableTaskVariable.createVarReplaceMap(this));

        if (isBuildSrc()) {
            return NbStrings.getBuildSrcMarker(rawName);
        }
        if (isRootProject()) {
            return NbStrings.getRootProjectMarker(rawName);
        }

        return rawName;
    }

    public String getDisplayName() {
        return getDisplayName(DisplayableTaskVariable.PROJECT_NAME.getScriptReplaceConstant());
    }

    public String getDescription() {
        if (isBuildSrc()) {
            File ownerProjectDir = getProjectDir().getParentFile();
            if (ownerProjectDir == null) {
                ownerProjectDir = getProjectDir();
            }

            String name = ownerProjectDir.getName();
            String projectDir = ownerProjectDir.getAbsolutePath();

            return NbStrings.getBuildSrcDescription(name, projectDir);
        }
        else {
            String name = getMainProject().getProjectFullName();
            name = name.trim();
            if (name.isEmpty()) {
                name = getProjectDir().getName();
            }

            String projectDir = getProjectDir().getAbsolutePath();

            if (isRootProject()) {
                return NbStrings.getRootProjectDescription(name, projectDir);
            }
            else {
                return NbStrings.getSubProjectDescription(name, projectDir);
            }
        }
    }

    public boolean isBuildSrc() {
        return getProjectDir().getName().equalsIgnoreCase(SettingsFiles.BUILD_SRC_NAME);
    }

    public boolean isRootProject() {
        return genericInfo.isRootProject();
    }

    public NbGradleMultiProjectDef getProjectDef() {
        return genericInfo.getProjectDef();
    }

    public NbGradleProjectTree getMainProject() {
        return genericInfo.getMainProject();
    }

    public NbGradleModel createNonDirtyCopy() {
        return new NbGradleModel(genericInfo, extensionModels);
    }

    public File getProjectDir() {
        return genericInfo.getProjectDir();
    }

    public File getRootProjectDir() {
        return genericInfo.getRootProjectDir();
    }

    public File getBuildFile() {
        return genericInfo.getBuildFile();
    }

    public File getSettingsFile() {
        return genericInfo.getSettingsFile();
    }

    public FileObject tryGetProjectDirAsObj() {
        return genericInfo.tryGetProjectDirAsObj();
    }

    public FileObject tryGetBuildFileObj() {
        return genericInfo.tryGetBuildFileObj();
    }

    public FileObject tryGetSettingsFileObj() {
        return genericInfo.tryGetSettingsFileObj();
    }

    public NbGradleModel updateEntry(NbGradleModel newContent) {
        Map<String, Object> newExtensionModels
                = new HashMap<>(extensionModels);

        for (Map.Entry<String, Object> entry: newContent.extensionModels.entrySet()) {
            newExtensionModels.put(entry.getKey(), entry.getValue());
        }

        return new NbGradleModel(newContent.getGenericInfo(), newExtensionModels, false);
    }
}
