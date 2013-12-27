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

    public static boolean isExceptionOfType(Throwable ex, String type) {
        Class<?> exType = ex.getClass();
        while (exType != null) {
            if (type.equals(exType.getName())) {
                return true;
            }

            exType = exType.getSuperclass();
        }
        return false;
    }

    public static boolean isExceptionOfSimpleType(Throwable ex, String type) {
        Class<?> exType = ex.getClass();
        while (exType != null) {
            if (type.equals(exType.getSimpleName())) {
                return true;
            }

            exType = exType.getSuperclass();
        }
        return false;
    }

    public static String getActualMessage(Throwable ex) {
        if (ex instanceof TransferableExceptionWrapper) {
            return ((TransferableExceptionWrapper)ex).getOriginalMessage();
        }
        else {
            return ex.getMessage();
        }
    }

    private Exceptions() {
        throw new AssertionError();
    }
}
