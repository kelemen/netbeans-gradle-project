package org.netbeans.gradle.project;

public final class Exceptions {
    public static RuntimeException throwUnchecked(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }
        else if (ex instanceof Error) {
            throw (Error)ex;
        }
        else {
            throw new RuntimeException(ex);
        }
    }

    private Exceptions() {
        throw new AssertionError();
    }
}
