package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class ModelQueryInput implements Serializable {
    private static final long serialVersionUID = 1L;

    // Object -> List of ProjectInfoBuilder<?>
    private final CustomSerializedMap.Deserializer projectInfoRequests;

    public ModelQueryInput(CustomSerializedMap.Deserializer projectInfoRequests) {
        if (projectInfoRequests == null) throw new NullPointerException("projectInfoRequests");
        this.projectInfoRequests = projectInfoRequests;
    }

    public Map<Object, List<?>> getProjectInfoRequests(SerializationCache cache, ClassLoader parent) {
        return projectInfoRequests.deserialize(
                cache,
                parent,
                projectInfoBuilderIssueTransformer());
    }

    public static IssueTransformer projectInfoBuilderIssueTransformer() {
        return ProjectInfoBuilderIssueTransformer.INSTANCE;
    }

    private static enum ProjectInfoBuilderIssueTransformer implements IssueTransformer {
        INSTANCE;

        @Override
        public Object transformIssue(Throwable issue) {
            return new FailingProjectInfoBuilder(issue);
        }
    }

    private static final class FailingProjectInfoBuilder implements ProjectInfoBuilder2<Void> {
        private static final long serialVersionUID = 1L;

        private final RuntimeException issue;

        public FailingProjectInfoBuilder(Throwable issue) {
            this.issue = TransferableExceptionWrapper.wrap(issue);
        }

        @Override
        public Void getProjectInfo(Object project) {
            throw issue;
        }

        @Override
        public String getName() {
            return "";
        }
    }
}
