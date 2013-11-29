package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectFullName;

    // Keys -> List of results of ProjectInfoBuilder
    private final CustomSerializedMap projectInfoResults;
    private final Throwable issue;

    public ModelQueryOutput(
            String projectFullName,
            CustomSerializedMap projectInfoResults,
            Throwable issue) {
        if (projectFullName == null) throw new NullPointerException("projectFullName");

        this.projectFullName = projectFullName;
        this.projectInfoResults = projectInfoResults;
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Throwable getIssue() {
        return issue;
    }

    public String getProjectFullName() {
        return projectFullName;
    }

    public CustomSerializedMap getProjectInfoResults() {
        return projectInfoResults;
    }
}
