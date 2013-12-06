package org.netbeans.gradle.model;

import java.io.Serializable;

public final class FetchedModelsOrError implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FetchedModels models;
    private final Throwable buildScriptEvaluationError;
    private final Throwable unexpectedError;

    public FetchedModelsOrError(
            FetchedModels models,
            Throwable buildScriptEvaluationError,
            Throwable unexpectedError) {

        this.models = models;
        this.buildScriptEvaluationError = buildScriptEvaluationError;
        this.unexpectedError = unexpectedError;
    }

    public FetchedModels getModels() {
        return models;
    }

    public Throwable getBuildScriptEvaluationError() {
        return buildScriptEvaluationError;
    }

    public Throwable getUnexpectedError() {
        return unexpectedError;
    }
}
