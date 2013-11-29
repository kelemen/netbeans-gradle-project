package org.netbeans.gradle.model.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TransferableExceptionWrapper extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String originalClassName;
    private final String originalMessage;

    public TransferableExceptionWrapper(Throwable wrapped) {
        super(wrap(wrapped.getCause()));

        this.originalClassName = wrapped.getClass().getName();
        this.originalMessage = wrapped.getMessage();
        setStackTrace(wrapped.getStackTrace());

        tryAddSuppressed(wrapped);
    }

    private void tryAddSuppressed(Throwable wrapped) {
        // In Java 7 there are suppressed exceptions which should be added as well.
        Method getSuppressed = ReflectionUtils.tryGetPublicMethod(
                wrapped.getClass(), "getSuppressed", Throwable[].class);
        if (getSuppressed == null) {
            return;
        }

        Method addSuppressed = ReflectionUtils.tryGetPublicMethod(
                getClass(), "addSuppressed", Void.TYPE, Throwable.class);
        if (addSuppressed == null) {
            return;
        }
        try {
            Throwable[] suppressedExceptions = (Throwable[])getSuppressed.invoke(wrapped);
            for (Throwable suppressedException: suppressedExceptions) {
                addSuppressed.invoke(this, wrap(suppressedException));
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw Exceptions.throwUnchecked(ex.getCause());
        }
    }

    public static TransferableExceptionWrapper wrap(Throwable exception) {
        if (exception == null) {
            return null;
        }

        if (exception instanceof TransferableExceptionWrapper) {
            return (TransferableExceptionWrapper)exception;
        }
        else {
            return new TransferableExceptionWrapper(exception);
        }
    }

    public String getOriginalClassName() {
        return originalClassName;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    @Override
    public String getMessage() {
        return originalClassName + ": " + originalMessage;
    }
}
