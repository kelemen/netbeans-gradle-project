package org.netbeans.gradle.model;

import java.io.Serializable;
import org.netbeans.gradle.model.util.Exceptions;

public final class BuilderResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Object resultObject;
    private final BuilderIssue issue;

    public BuilderResult(Object resultObject, BuilderIssue issue) {
        this.resultObject = resultObject;
        this.issue = issue;
    }

    public Object getResultObject() {
        return resultObject;
    }

    public BuilderIssue getIssue() {
        return issue;
    }

    public Object getResultIfNoIssue() {
        if (issue != null) {
            throw Exceptions.throwUnchecked(issue.getException());
        }
        return resultObject;
    }
}
