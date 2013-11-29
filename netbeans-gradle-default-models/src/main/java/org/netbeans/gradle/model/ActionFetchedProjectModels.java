package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

final class ActionFetchedProjectModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GradleMultiProjectDef projectDef;
    private final CustomSerializedMap projectInfoResults;
    private final Map<Class<?>, Object> toolingModels;
    private final Throwable issue;

    public ActionFetchedProjectModels(
            GradleMultiProjectDef projectDef,
            CustomSerializedMap projectInfoResults,
            Map<Class<?>, Object> toolingModels,
            Throwable issue) {
        if (projectDef == null) throw new NullPointerException("projectDef");
        if (projectInfoResults == null) throw new NullPointerException("projectInfoResults");

        this.projectDef = projectDef;
        this.projectInfoResults = projectInfoResults;
        this.toolingModels = CollectionUtils.copyNullSafeHashMap(toolingModels);
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Throwable getIssue() {
        return issue;
    }

    public GradleMultiProjectDef getProjectDef() {
        return projectDef;
    }

    public CustomSerializedMap getProjectInfoResults() {
        return projectInfoResults;
    }

    public Map<Class<?>, Object> getToolingModels() {
        return toolingModels;
    }
}
