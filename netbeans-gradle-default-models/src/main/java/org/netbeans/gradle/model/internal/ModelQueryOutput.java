package org.netbeans.gradle.model.internal;

import java.io.Serializable;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectFullName;

    // Keys -> List of results of ProjectInfoBuilder
    private final CustomSerializedMap projectInfoResults;

    public ModelQueryOutput(String projectFullName, CustomSerializedMap projectInfoResults) {
        if (projectFullName == null) throw new NullPointerException("projectFullName");

        this.projectFullName = projectFullName;
        this.projectInfoResults = projectInfoResults;
    }

    public String getProjectFullName() {
        return projectFullName;
    }

    public CustomSerializedMap getProjectInfoResults() {
        return projectInfoResults;
    }
}
