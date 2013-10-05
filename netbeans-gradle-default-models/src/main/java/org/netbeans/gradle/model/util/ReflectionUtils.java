package org.netbeans.gradle.model.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtils {
    private static final Object[] EMPTY_ARR = new Object[0];

    private static Object callParameterLessMethodSimple(Object obj, String methodName) throws NoSuchMethodException {
        try {
            Method method = obj.getClass().getMethod(methodName);
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
            Method invokeMethod = dynObj.getClass().getMethod("invokeMethod", String.class, Object[].class);
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

    private ReflectionUtils() {
        throw new AssertionError();
    }
}
