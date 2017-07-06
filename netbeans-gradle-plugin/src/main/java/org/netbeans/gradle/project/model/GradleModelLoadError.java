package org.netbeans.gradle.project.model;

import java.util.Objects;
import org.netbeans.gradle.project.NbGradleProject;

@SuppressWarnings("serial") // Not serializable
public final class GradleModelLoadError extends Exception {
    private final NbGradleProject project;
    private final Throwable buildScriptEvaluationError;
    private final Throwable unexpectedError;

    public GradleModelLoadError(
            NbGradleProject project,
            Throwable buildScriptEvaluationError,
            Throwable unexpectedError) {

        super(pickNonNull(unexpectedError, buildScriptEvaluationError));

        this.project = Objects.requireNonNull(project, "project");
        this.buildScriptEvaluationError = buildScriptEvaluationError;
        this.unexpectedError = unexpectedError;
    }

    public static GradleModelLoadError fromBuildScriptError(
            NbGradleProject project,
            Throwable buildScriptEvaluationError) {
        return new GradleModelLoadError(project, buildScriptEvaluationError, null);
    }

    public static GradleModelLoadError fromUnexpectedError(
            NbGradleProject project,
            Throwable unexpectedError) {
        return new GradleModelLoadError(project, null, unexpectedError);
    }

    public Throwable getBuildScriptEvaluationError() {
        return buildScriptEvaluationError;
    }

    public Throwable getUnexpectedError() {
        return unexpectedError;
    }

    public NbGradleProject getProject() {
        return project;
    }

    private static Throwable pickNonNull(Throwable... errors) {
        for (Throwable error: errors) {
            if (error != null) {
                return error;
            }
        }

        throw new IllegalArgumentException("One of exceptions must be non-null.");
    }
}
