package org.netbeans.gradle.model;

import java.io.Serializable;
import org.netbeans.gradle.model.util.TransferableExceptionWrapper;

public final class BuilderIssue implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Throwable issue;

    public BuilderIssue(String name, Throwable exception) {
        if (name == null) throw new NullPointerException("name");
        if (exception == null) throw new NullPointerException("exception");

        this.name = name;
        this.issue = TransferableExceptionWrapper.wrap(exception);
    }

    public String getName() {
        return name;
    }

    public Throwable getException() {
        return issue;
    }
}
