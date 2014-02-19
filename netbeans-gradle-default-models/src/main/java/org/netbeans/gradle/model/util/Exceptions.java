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
        String wrappedName = tryGetWrappedClassName(ex);
        if (type.equals(wrappedName)) {
            return true;
        }

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
        String wrappedName = tryGetWrappedClassName(ex);
        if (type.equals(wrappedName)) {
            return true;
        }

        Class<?> exType = ex.getClass();
        while (exType != null) {
            if (type.equals(exType.getSimpleName())) {
                return true;
            }

            exType = exType.getSuperclass();
        }
        return false;
    }

    private static String tryGetWrappedClassName(Throwable ex) {
        if (ex instanceof TransferableExceptionWrapper) {
            return ((TransferableExceptionWrapper)ex).getOriginalClassName();
        }
        else {
            return null;
        }
    }

    public static String getActualMessage(Throwable ex) {
        if (ex instanceof TransferableExceptionWrapper) {
            return ((TransferableExceptionWrapper)ex).getOriginalMessage();
        }
        else {
            return ex.getMessage();
        }
    }

    public static Throwable getRootCause(Throwable error) {
        if (error == null) throw new NullPointerException("error");

        Throwable parent;
        Throwable cause = error;

        do {
            parent = cause;
            cause = parent.getCause();
        } while (cause != null);

        return parent;
    }

    private Exceptions() {
        throw new AssertionError();
    }
}
