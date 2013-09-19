package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedBuildModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, Object> buildInfoResults;

    public FetchedBuildModels(Map<Object, Object> buildInfoResults) {
        this.buildInfoResults = CollectionUtils.copyNullSafeHashMap(buildInfoResults);
    }

    public Map<Object, Object> getBuildInfoResults() {
        return buildInfoResults;
    }
}
