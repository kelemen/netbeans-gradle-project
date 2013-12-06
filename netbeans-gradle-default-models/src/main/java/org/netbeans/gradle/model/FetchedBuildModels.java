package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedBuildModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, List<BuilderResult>> buildInfoResults;

    public FetchedBuildModels(Map<Object, List<?>> buildInfoResults) {
        this.buildInfoResults = CollectionUtils.copyNullSafeMultiHashMapReified(
                BuilderResult.class, buildInfoResults);
    }

    public Map<Object, List<BuilderResult>> getBuildInfoResults() {
        return buildInfoResults;
    }
}
