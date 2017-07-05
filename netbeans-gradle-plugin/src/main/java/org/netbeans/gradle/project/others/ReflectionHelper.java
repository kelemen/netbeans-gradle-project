package org.netbeans.gradle.project.others;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.util.Exceptions;

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

    private static boolean areParamsOk(Class<?>[] paramTypes, Map<Class<?>, Object> args) {
        for (Class<?> paramType: paramTypes) {
            if (!args.containsKey(paramType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublic(Member member) {
        return Modifier.isPublic(member.getModifiers());
    }

    public static Object tryCreateInstance(Class<?> cl, Map<Class<?>, Object> args) {
        for (Constructor<?> constructor: cl.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();

            if (isPublic(constructor) && areParamsOk(paramTypes, args)) {
                Object[] passedArgs = new Object[paramTypes.length];
                for (int i = 0; i < passedArgs.length; i++) {
                    passedArgs[i] = args.get(paramTypes[i]);
                }

                try {
                    return constructor.newInstance(passedArgs);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
                    LOGGER.log(Level.WARNING, "Unexpected consturctor invocation error.", ex);
                } catch (InvocationTargetException ex) {
                    throw Exceptions.throwUnchecked(ex.getCause());
                }

                return null;
            }
        }

        return null;
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
            throw Exceptions.throwUnchecked(ex.getCause());
        }
        return null;
    }

    public static ClassFinder constClassFinder(Class<?> type) {
        return () -> type;
    }

    public static ClassFinder[] constClassFinders(Class<?>... types) {
        ClassFinder[] result = new ClassFinder[types.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = constClassFinder(types[i]);
        }
        return result;
    }
}
