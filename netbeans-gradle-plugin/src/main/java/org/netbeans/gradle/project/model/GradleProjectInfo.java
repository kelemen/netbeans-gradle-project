package org.netbeans.gradle.project.model;

import org.netbeans.gradle.model.GradleMultiProjectDef;

public interface GradleProjectInfo {
    public GradleMultiProjectDef getProjectDef();

    public <T> T tryGetModel(Class<T> modelClass);
    public Object tryGetProjectInfoResult(Object key);
}
