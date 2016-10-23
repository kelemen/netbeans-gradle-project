package org.netbeans.gradle.model.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class ReflectionUtils {
    private static final Object[] EMPTY_ARR = new Object[0];

    public static String updateTypeName(Class<?> defaultPackage, String typeName) {
        if (typeName.indexOf('.') >= 0) {
            return typeName;
        }
        return defaultPackage.getPackage().getName() + "." + typeName;
    }

    public static Method getAccessibleMethod(Class<?> type, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Method method: type.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                continue;
            }
            if (!methodName.equals(method.getName())) {
                continue;
            }
            if (!Arrays.equals(parameterTypes, method.getParameterTypes())) {
                continue;
            }

            return method;
        }

        Method method = type.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static Object callParameterLessMethodSimple(Object obj, String methodName) throws NoSuchMethodException {
        try {
            Method method = getAccessibleMethod(obj.getClass(), methodName);
            return method.invoke(obj);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Object callParameterLessMethodDyn(Object obj, String methodName) throws NoSuchMethodException {
        try {
            Object dynObj = callParameterLessMethodSimple(obj, "getAsDynamicObject");
            Method invokeMethod = getAccessibleMethod(dynObj.getClass(), "invokeMethod", String.class, Object[].class);
            Object result = invokeMethod.invoke(dynObj, methodName, EMPTY_ARR);

            return result;
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Object callParameterLessMethodSimpleFirst(Object obj, String methodName) {
        try {
            return callParameterLessMethodSimple(obj, methodName);
        } catch (NoSuchMethodException ex) {
            try {
                return callParameterLessMethodDyn(obj, methodName);
            } catch (NoSuchMethodException ex2) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static Object callParameterLessMethodDynFirst(Object obj, String methodName) {
        try {
            return callParameterLessMethodDyn(obj, methodName);
        } catch (NoSuchMethodException ex) {
            try {
                return callParameterLessMethodSimple(obj, methodName);
            } catch (NoSuchMethodException ex2) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static String getGetterOfProperty(String prefix, String propertyName) {
        StringBuilder methodName = new StringBuilder(propertyName.length() + prefix.length());
        methodName.append(prefix);
        methodName.append(Character.toUpperCase(propertyName.charAt(0)));
        methodName.append(propertyName.substring(1));

        return methodName.toString();
    }

    private static Object getPropertySimpleFirst(Object obj, String prefix, String propertyName) {
        String methodName = getGetterOfProperty(prefix, propertyName);
        return callParameterLessMethodSimpleFirst(obj, methodName);
    }

    private static Object getPropertyDynFirst(Object obj, String prefix, String propertyName) {
        String methodName = getGetterOfProperty(prefix, propertyName);
        return callParameterLessMethodDynFirst(obj, methodName);
    }

    public static Object getNonBoolProperty(Object obj, String propertyName) {
        return getPropertySimpleFirst(obj, "get", propertyName);
    }

    public static boolean getBoolProperty(Object obj, String propertyName) {
        return (Boolean)getPropertySimpleFirst(obj, "is", propertyName);
    }

    public static Object getNonBoolPropertyDyn(Object obj, String propertyName) {
        return getPropertyDynFirst(obj, "get", propertyName);
    }

    public static boolean getBoolPropertyDyn(Object obj, String propertyName) {
        return (Boolean)getPropertyDynFirst(obj, "is", propertyName);
    }

    public static String getStringProperty(Object obj, String propertyName) {
        Object result = getNonBoolProperty(obj, propertyName);
        return result != null ? result.toString() : null;
    }

    public static boolean isPublic(Member member) {
        int modifiers = member.getModifiers();
        return (modifiers & Modifier.PUBLIC) != 0;
    }

    public static Method tryGetPublicMethod(
            Class<?> type,
            String methodName,
            Class<?> returnType,
            Class<?>... argTypes) {

        try {
            Method method = getAccessibleMethod(type, methodName, argTypes);
            if (!isPublic(method)) {
                return null;
            }

            if (!returnType.isAssignableFrom(method.getReturnType())) {
                return null;
            }

            if (Modifier.isAbstract(method.getModifiers())) {
                return null;
            }

            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private ReflectionUtils() {
        throw new AssertionError();
    }
}
