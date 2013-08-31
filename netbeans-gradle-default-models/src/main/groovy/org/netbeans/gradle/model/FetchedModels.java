package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, Object> buildInfoResults;
    private final Map<Object, Object> projectInfoResults;

    public FetchedModels(Map<Object, Object> buildInfoResults, Map<Object, Object> projectInfoResults) {
        this.buildInfoResults = CollectionUtils.copyNullSafeHashMap(buildInfoResults);
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public Map<Object, Object> getBuildInfoResults() {
        return buildInfoResults;
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}
