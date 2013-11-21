package org.netbeans.gradle.project.model;

import java.util.List;
import org.netbeans.gradle.model.GradleMultiProjectDef;

public interface GradleProjectInfo {
    public GradleMultiProjectDef getProjectDef();

    public <T> T tryGetModel(Class<T> modelClass);
    public List<?> tryGetProjectInfoResult(Object key);
}
