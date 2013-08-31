package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, Object> projectInfoResults;

    public ModelQueryOutput(Map<Object, Object> projectInfoResults) {
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}
