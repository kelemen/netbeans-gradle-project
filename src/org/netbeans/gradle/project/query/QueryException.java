package org.netbeans.gradle.project.query;

public class QueryException extends Exception {
    private static final long serialVersionUID = -7864015319726547616L;

    public QueryException() {
    }

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryException(Throwable cause) {
        super(cause);
    }
}
