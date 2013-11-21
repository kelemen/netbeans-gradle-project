package org.netbeans.gradle.project.model;

import java.util.List;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.MultiKey;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class GradleProjectInfoOfExtension implements GradleProjectInfo {
    private final String extensionName;
    private final FetchedProjectModels models;

    public GradleProjectInfoOfExtension(String extensionName, FetchedProjectModels models) {
        if (extensionName == null) throw new NullPointerException("extensionName");
        if (models == null) throw new NullPointerException("models");

        this.extensionName = extensionName;
        this.models = models;
    }

    @Override
    public <T> T tryGetModel(Class<T> modelClass) {
        Object result = models.getToolingModels().get(modelClass);
        return modelClass.cast(result);
    }

    @Override
    public List<?> tryGetProjectInfoResult(Object key) {
        return models.getProjectInfoResults().get(MultiKey.create(extensionName, key));
    }

    @Override
    public GradleMultiProjectDef getProjectDef() {
        return models.getProjectDef();
    }
}
