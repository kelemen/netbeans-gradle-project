package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedBuildModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, List<?>> buildInfoResults;

    public FetchedBuildModels(Map<Object, List<?>> buildInfoResults) {
        this.buildInfoResults = CollectionUtils.copyNullSafeMultiHashMap(buildInfoResults);
    }

    public Map<Object, List<?>> getBuildInfoResults() {
        return buildInfoResults;
    }
}
