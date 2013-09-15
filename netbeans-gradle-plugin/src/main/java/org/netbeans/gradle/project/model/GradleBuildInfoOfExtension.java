package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.gradle.model.FetchedBuildModels;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.MultiKey;

public final class GradleBuildInfoOfExtension implements GradleBuildInfo {
    private final String extensionName;
    private final FetchedBuildModels buildModels;
    private final GradleProjectInfo defaultInfo;
    private final Map<File, GradleProjectInfo> allProjectInfos;

    public GradleBuildInfoOfExtension(String extensionName, FetchedModels models) {
        if (extensionName == null) throw new NullPointerException("extensionName");
        if (models == null) throw new NullPointerException("models");

        this.extensionName = extensionName;
        this.buildModels = models.getBuildModels();
        this.defaultInfo = new GradleProjectInfoOfExtension(extensionName, models.getDefaultProjectModels());
        this.allProjectInfos = allInfos(extensionName, models);
    }

    private GradleBuildInfoOfExtension(GradleBuildInfoOfExtension base, GradleProjectInfo defaultProject) {
        this.extensionName = base.extensionName;
        this.buildModels = base.buildModels;
        this.defaultInfo = defaultProject;
        this.allProjectInfos = base.allProjectInfos;
    }

    private static void addProjectModels(
            String extensionName,
            FetchedProjectModels models,
            Map<File, GradleProjectInfo> result) {

        File projectDir = models.getProjectDef().getMainProject().getGenericProperties().getProjectDir();
        result.put(projectDir, new GradleProjectInfoOfExtension(extensionName, models));
    }

    private static Map<File, GradleProjectInfo> allInfos(String extensionName, FetchedModels models) {
        Map<File, GradleProjectInfo> result = new HashMap<File, GradleProjectInfo>();

        for (FetchedProjectModels otherModels: models.getOtherProjectModels()) {
            addProjectModels(extensionName, otherModels, result);
        }

        addProjectModels(extensionName, models.getDefaultProjectModels(), result);

        return Collections.unmodifiableMap(result);
    }

    public GradleBuildInfo tryGetViewOfOtherProject(File projectDir) {
        GradleProjectInfo other = allProjectInfos.get(projectDir);
        if (other == null) {
            return null;
        }
        return new GradleBuildInfoOfExtension(this, other);
    }

    @Override
    public GradleProjectInfo getDefaultProjectInfo() {
        return defaultInfo;
    }

    @Override
    public Object tryGetBuildInfoResult(Object key) {
        return buildModels.getBuildInfoResults().get(MultiKey.create(extensionName, key));
    }

    @Override
    public Map<File, GradleProjectInfo> getAllProjectInfos() {
        return allProjectInfos;
    }
}
