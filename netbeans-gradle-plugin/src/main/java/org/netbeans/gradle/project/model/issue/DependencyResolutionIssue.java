package org.netbeans.gradle.project.model.issue;

import java.util.Objects;
import org.netbeans.gradle.project.NbStrings;

public final class DependencyResolutionIssue {
    public enum DependencyKind {
        RUNTIME,
        COMPILE;
    }

    private final String projectName;
    private final String sourceSetName;
    private final DependencyKind dependencyKind;
    private final Throwable stackTrace;

    public DependencyResolutionIssue(
            String projectName,
            String sourceSetName,
            DependencyKind dependencyKind,
            Throwable stackTrace) {

        this.projectName = Objects.requireNonNull(projectName, "projectName");
        this.sourceSetName = Objects.requireNonNull(sourceSetName, "sourceSetName");
        this.dependencyKind = Objects.requireNonNull(dependencyKind, "dependencyKind");
        this.stackTrace = Objects.requireNonNull(stackTrace, "stackTrace");
    }

    public static DependencyResolutionIssue compileIssue(String projectName, String sourceSetName, Throwable stackTrace) {
        return new DependencyResolutionIssue(projectName, sourceSetName, DependencyKind.COMPILE, stackTrace);
    }

    public static DependencyResolutionIssue runtimeIssue(String projectName, String sourceSetName, Throwable stackTrace) {
        return new DependencyResolutionIssue(projectName, sourceSetName, DependencyKind.RUNTIME, stackTrace);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getSourceSetName() {
        return sourceSetName;
    }

    public DependencyKind getDependencyKind() {
        return dependencyKind;
    }

    public Throwable getStackTrace() {
        return stackTrace;
    }

    public String getMessage() {
        switch (dependencyKind) {
            case RUNTIME:
                return NbStrings.getRuntimeDependencyResolutionFailure(projectName, sourceSetName);
            case COMPILE:
                return NbStrings.getCompileDependencyResolutionFailure(projectName, sourceSetName);
            default:
                throw new AssertionError(dependencyKind.name());
        }
    }
}
