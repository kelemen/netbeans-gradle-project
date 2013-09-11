package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedProjectModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GradleMultiProjectDef projectDef;
    private final Map<Object, Object> projectInfoResults;

    public FetchedProjectModels(
            GradleMultiProjectDef projectDef,
            Map<Object, Object> projectInfoResults) {
        if (projectDef == null) throw new NullPointerException("projectDef");

        this.projectDef = projectDef;
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public GradleMultiProjectDef getProjectDef() {
        return projectDef;
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}
