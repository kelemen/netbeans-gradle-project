package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kelemen Attila
 */
public final class ReflectionHelper {
    private static final Logger LOGGER = Logger.getLogger(ReflectionHelper.class.getName());

    public static Method tryGetMethod(Class<?> cl, String methodName, Class<?>... args) {
        try {
            return cl.getMethod(methodName, args);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static Object tryInvoke(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException ex) {
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Unexpected IllegalArgumentException.", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            if (cause instanceof Error) {
                throw (Error)cause;
            }
            throw new RuntimeException(cause);
        }
        return null;
    }
}
