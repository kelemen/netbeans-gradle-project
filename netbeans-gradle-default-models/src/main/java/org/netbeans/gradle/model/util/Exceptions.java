package org.netbeans.gradle.model.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Exceptions {
    private static final Logger LOGGER = Logger.getLogger(Exceptions.class.getName());

    private static void logSuppressedErrors(Throwable[] suppressedErrors) {
        for (Throwable suppressed: suppressedErrors) {
            LOGGER.log(Level.WARNING, "Suppressing exception.", suppressed);
        }
    }

    public static void tryAddSuppressedException(Throwable main, Throwable... suppressedErrors) {
        if (main == null) throw new NullPointerException("main");
        if (suppressedErrors == null) throw new NullPointerException("suppressedErrors");

        if (suppressedErrors.length == 0) {
            return;
        }

        Method addSuppressed = ReflectionUtils.tryGetPublicMethod(Throwable.class, "addSuppressed", Void.TYPE, Throwable.class);
        if (addSuppressed == null) {
            logSuppressedErrors(suppressedErrors);
        }
        else {
            for (Throwable suppressed: suppressedErrors) {
                try {
                    addSuppressed.invoke(main, suppressed);
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, "Failed to call Throwable.addSuppressed", ex);
                    LOGGER.log(Level.WARNING, "Suppressing exception.", suppressed);
                }
            }
        }
    }

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
