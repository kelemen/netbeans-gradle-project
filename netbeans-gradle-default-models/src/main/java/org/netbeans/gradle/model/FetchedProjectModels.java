package org.netbeans.gradle.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class FetchedProjectModels implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GradleMultiProjectDef projectDef;
    private final Map<Object, List<BuilderResult>> projectInfoResults;
    private final Map<Class<?>, Object> toolingModels;
    private final Throwable issue;

    public FetchedProjectModels(
            GradleMultiProjectDef projectDef,
            Map<Object, List<?>> projectInfoResults,
            Map<Class<?>, Object> toolingModels,
            Throwable issue) {
        if (projectDef == null) throw new NullPointerException("projectDef");

        this.projectDef = projectDef;
        this.projectInfoResults = CollectionUtils.copyNullSafeMultiHashMapReified(
                BuilderResult.class, projectInfoResults);

        this.toolingModels = CollectionUtils.copyNullSafeHashMap(toolingModels);
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Throwable getIssue() {
        return issue;
    }

    public GradleMultiProjectDef getProjectDef() {
        return projectDef;
    }

    public Map<Object, List<BuilderResult>> getProjectInfoResults() {
        return projectInfoResults;
    }

    public Map<Class<?>, Object> getToolingModels() {
        return toolingModels;
    }
}
