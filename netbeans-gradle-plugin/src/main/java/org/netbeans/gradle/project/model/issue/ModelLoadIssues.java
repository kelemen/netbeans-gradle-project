package org.netbeans.gradle.project.model.issue;

import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.project.NbGradleExtensionRef;

public final class ModelLoadIssues {
    public static ModelLoadIssue internalError(String issueDescription, Throwable stackTrace) {
        return new ModelLoadIssueImpl(ModelLoadIssue.Severity.INTERNAL_ERROR, issueDescription, stackTrace);
    }

    public static ModelLoadIssue extensionError(String issueDescription, Throwable stackTrace) {
        return new ModelLoadIssueImpl(ModelLoadIssue.Severity.EXTENSION_ERROR, issueDescription, stackTrace);
    }

    public static ModelLoadIssue buildScriptError(String issueDescription, Throwable stackTrace) {
        return new ModelLoadIssueImpl(ModelLoadIssue.Severity.BUILD_SCRIPT_ERROR, issueDescription, stackTrace);
    }

    public static ModelLoadIssue projectModelLoadError(
            FetchedProjectModels project,
            Throwable issue) {
        if (project == null) throw new NullPointerException("project");
        if (issue == null) throw new NullPointerException("issue");

        // TODO: I18N
        GenericProjectProperties projectID = project.getProjectDef().getMainProject().getGenericProperties();
        String projectName = projectID.getProjectFullName() + " in " + projectID.getProjectDir();
        String issueDescription =
                "Fetching information for project has failed with an unexpected error "
                + projectName;

        return internalError(issueDescription, issue);
    }

    public static ModelLoadIssue builderError(
            FetchedProjectModels project,
            NbGradleExtensionRef extensionRef,
            BuilderIssue issue) {
        if (project == null) throw new NullPointerException("project");
        if (extensionRef == null) throw new NullPointerException("extensionRef");
        if (issue == null) throw new NullPointerException("issue");

        // TODO: I18N
        GenericProjectProperties projectID = project.getProjectDef().getMainProject().getGenericProperties();
        String projectName = projectID.getProjectFullName() + " in " + projectID.getProjectDir();
        String issueDescription =
                "Fetching information for "
                + extensionRef.getDisplayName()
                + " [" + issue.getName() + "]"
                + " has failed for project "
                + projectName;

        return extensionError(issueDescription, issue.getException());
    }

    private static final class ModelLoadIssueImpl implements ModelLoadIssue {
        private final Severity severity;
        private final String issueDescription;
        private final Throwable stackTrace;

        public ModelLoadIssueImpl(Severity severity, String issueDescription, Throwable stackTrace) {
            if (severity == null) throw new NullPointerException("severity");
            if (issueDescription == null) throw new NullPointerException("issueDescription");
            if (stackTrace == null) throw new NullPointerException("stackTrace");

            this.severity = severity;
            this.issueDescription = issueDescription;
            this.stackTrace = stackTrace;
        }

        @Override
        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String getIssueDescription() {
            return issueDescription;
        }

        @Override
        public Throwable getStackTrace() {
            return stackTrace;
        }
    }
}
