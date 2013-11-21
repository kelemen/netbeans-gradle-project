package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class ModelQueryInput implements Serializable {
    private static final long serialVersionUID = 1L;

    // Object -> List of ProjectInfoBuilder<?>
    private final CustomSerializedMap.Deserializer projectInfoRequests;

    public ModelQueryInput(CustomSerializedMap.Deserializer projectInfoRequests) {
        if (projectInfoRequests == null) throw new NullPointerException("projectInfoRequests");
        this.projectInfoRequests = projectInfoRequests;
    }

    public Map<Object, List<?>> getProjectInfoRequests(ClassLoader parent) {
        return projectInfoRequests.deserialize(parent);
    }
}
