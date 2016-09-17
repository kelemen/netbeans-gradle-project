package org.netbeans.gradle.project.model.issue;

import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;

public final class ModelLoadIssues {
    public static ModelLoadIssue projectModelLoadError(
            NbGradleProject requestedProject,
            FetchedProjectModels project,
            NbGradleExtensionRef extension,
            Throwable issue) {

        GenericProjectProperties projectID = project.getProjectDef().getMainProject().getGenericProperties();
        return new ModelLoadIssue(requestedProject, projectID.getProjectFullName(), extension, null, issue);
    }

    public static ModelLoadIssue builderError(
            NbGradleProject requestedProject,
            FetchedProjectModels project,
            NbGradleExtensionRef extensionRef,
            BuilderIssue issue) {

        GenericProjectProperties projectID = project.getProjectDef().getMainProject().getGenericProperties();

        return new ModelLoadIssue(
                requestedProject,
                projectID.getProjectFullName(),
                extensionRef,
                issue.getName(),
                issue.getException());
    }
}
