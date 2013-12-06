package org.netbeans.gradle.project.model;

public final class GradleModelLoadError extends Exception {
    private static final long serialVersionUID = 1L;

    private final Throwable buildScriptEvaluationError;
    private final Throwable unexpectedError;

    public GradleModelLoadError(Throwable buildScriptEvaluationError, Throwable unexpectedError) {
        super(pickNonNull(unexpectedError, buildScriptEvaluationError));

        this.buildScriptEvaluationError = buildScriptEvaluationError;
        this.unexpectedError = unexpectedError;
    }

    public Throwable getBuildScriptEvaluationError() {
        return buildScriptEvaluationError;
    }

    public Throwable getUnexpectedError() {
        return unexpectedError;
    }

    private static Throwable pickNonNull(Throwable... errors) {
        for (Throwable error: errors) {
            if (error != null) {
                return error;
            }
        }

        throw new IllegalArgumentException("One of exceptions must be null.");
    }
}
