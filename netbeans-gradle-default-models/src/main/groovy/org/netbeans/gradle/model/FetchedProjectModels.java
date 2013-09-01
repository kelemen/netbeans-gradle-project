package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class FetchedProjectModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties genericProperties;
    private final Map<Object, Object> projectInfoResults;

    public FetchedProjectModels(
            GenericProjectProperties genericProperties,
            Map<Object, Object> projectInfoResults) {
        if (genericProperties == null) throw new NullPointerException("genericProperties");

        this.genericProperties = genericProperties;
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public GenericProjectProperties getGenericProperties() {
        return genericProperties;
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}
