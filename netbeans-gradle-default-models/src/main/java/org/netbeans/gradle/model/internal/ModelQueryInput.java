package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class ModelQueryInput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Object, ProjectInfoBuilder<?>> projectInfoRequests;

    public ModelQueryInput(Map<Object, ProjectInfoBuilder<?>> projectInfoRequests) {
        this.projectInfoRequests = CollectionUtils.copyNullSafeHashMap(projectInfoRequests);
    }

    public Map<Object, ProjectInfoBuilder<?>> getProjectInfoRequests() {
        return projectInfoRequests;
    }
}
