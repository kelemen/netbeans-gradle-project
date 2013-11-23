package org.netbeans.gradle.model.util;

import java.io.IOException;

public final class Exceptions {
    public static RuntimeException throwUncheckedIO(Throwable ex) throws IOException {
        if (ex instanceof IOException) {
            throw (IOException)ex;
        }
        throw throwUnchecked(ex);
    }

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
