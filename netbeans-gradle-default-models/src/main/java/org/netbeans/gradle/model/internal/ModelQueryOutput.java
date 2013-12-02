package org.netbeans.gradle.model.internal;

import java.io.File;
import java.io.Serializable;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectFullName;
    private final File buildScript;

    // Keys -> List of results of ProjectInfoBuilder
    private final CustomSerializedMap projectInfoResults;
    private final Throwable issue;

    public ModelQueryOutput(
            String projectFullName,
            File buildScript,
            CustomSerializedMap projectInfoResults,
            Throwable issue) {
        if (projectFullName == null) throw new NullPointerException("projectFullName");

        this.projectFullName = projectFullName;
        this.buildScript = buildScript;
        this.projectInfoResults = projectInfoResults;
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Throwable getIssue() {
        return issue;
    }

    public String getProjectFullName() {
        return projectFullName;
    }

    public File getBuildScript() {
        return buildScript;
    }

    public CustomSerializedMap getProjectInfoResults() {
        return projectInfoResults;
    }
}
