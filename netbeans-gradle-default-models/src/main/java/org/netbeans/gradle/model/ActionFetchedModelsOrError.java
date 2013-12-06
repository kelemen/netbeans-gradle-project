package org.netbeans.gradle.model;

import java.io.Serializable;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

final class ActionFetchedModelsOrError implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ActionFetchedModels models;
    private final Throwable buildScriptEvaluationError;
    private final Throwable unexpectedError;

    public ActionFetchedModelsOrError(
            ActionFetchedModels models,
            Throwable buildScriptEvaluationError,
            Throwable unexpectedError) {

        this.models = models;
        this.buildScriptEvaluationError = TransferableExceptionWrapper.wrap(buildScriptEvaluationError);
        this.unexpectedError = TransferableExceptionWrapper.wrap(unexpectedError);
    }

    public ActionFetchedModels getModels() {
        return models;
    }

    public Throwable getBuildScriptEvaluationError() {
        return buildScriptEvaluationError;
    }

    public Throwable getUnexpectedError() {
        return unexpectedError;
    }
}
