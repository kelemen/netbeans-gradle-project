package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kelemen Attila
 */
public final class ReflectionHelper {
    private static final Logger LOGGER = Logger.getLogger(ReflectionHelper.class.getName());

    public static boolean isInstanceOf(Object obj, String requiredTypeName) {
        return isInstanceOfAny(obj, Collections.singleton(requiredTypeName));
    }

    private static boolean isTypeOfAny(Class<?> type, Set<String> requiredTypeNames) {
        if (requiredTypeNames.contains(type.getName())) {
            return true;
        }

        for (Class<?> implementedIF: type.getInterfaces()) {
            if (requiredTypeNames.contains(implementedIF.getName())) {
                return true;
            }
        }

        Class<?> superclass = type.getSuperclass();
        return superclass != null
                ? isTypeOfAny(superclass, requiredTypeNames)
                : false;
    }

    public static boolean isInstanceOfAny(Object obj, Set<String> requiredTypeNames) {
        if (obj == null) {
            return false;
        }

        return isTypeOfAny(obj.getClass(), requiredTypeNames);
    }

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
