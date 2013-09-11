package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectFullName;
    private final Map<Object, Object> projectInfoResults;

    public ModelQueryOutput(String projectFullName, Map<Object, Object> projectInfoResults) {
        if (projectFullName == null) throw new NullPointerException("projectFullName");

        this.projectFullName = projectFullName;
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public String getProjectFullName() {
        return projectFullName;
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}
