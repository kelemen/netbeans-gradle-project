package org.netbeans.gradle.model;

import java.io.Serializable;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class BuilderResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Object resultObject;
    private final Throwable issue;

    public BuilderResult(Object resultObject, Throwable issue) {
        this.resultObject = resultObject;
        this.issue = TransferableExceptionWrapper.wrap(issue);
    }

    public Object getResultObject() {
        return resultObject;
    }

    public Throwable getIssue() {
        return issue;
    }

    public Object getResultIfNoIssue() {
        if (issue != null) {
            throw Exceptions.throwUnchecked(issue);
        }
        return resultObject;
    }
}
