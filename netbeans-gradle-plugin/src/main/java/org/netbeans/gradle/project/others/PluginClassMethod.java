package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class PluginClassMethod {
    private final ClassFinder pluginClass;
    private final String methodName;
    private final ClassFinder[] argTypeFinders;
    private final AtomicReference<Method> methodRef;

    @SuppressWarnings("VolatileArrayField")
    private volatile Class<?>[] argTypesCache;

    public PluginClassMethod(ClassFinder pluginClass, String methodName, Class<?>... argTypes) {
        this(pluginClass, methodName, ReflectionHelper.constClassFinders(argTypes));
    }

    public PluginClassMethod(ClassFinder pluginClass, String methodName, ClassFinder... argTypeFinders) {
        if (pluginClass == null) throw new NullPointerException("pluginClass");
        if (methodName == null) throw new NullPointerException("methodName");

        this.pluginClass = pluginClass;
        this.methodName = methodName;
        this.argTypeFinders = argTypeFinders.clone();
        this.methodRef = new AtomicReference<Method>(null);
        this.argTypesCache = null;

        CollectionUtils.checkNoNullElements(Arrays.asList(this.argTypeFinders), "argTypeFinders");
    }

    private Class<?>[] findArgTypes() {
        Class<?>[] result = new Class<?>[argTypeFinders.length];
        for (int i = 0; i < result.length; i++) {
            Class<?> argType = argTypeFinders[i].tryGetClass();
            if (argType == null) {
                return null;
            }

            result[i] = argTypeFinders[i].tryGetClass();
        }
        return result;
    }

    private Class<?>[] getArgTypes() {
        Class<?>[] result = argTypesCache;
        if (result == null) {
            result = findArgTypes();
            if (result != null) {
                argTypesCache = result;
            }
        }
        return result;
    }

    public Method tryGetMethod() {
        Method result = methodRef.get();
        if (result == null) {
            methodRef.compareAndSet(null, tryFindMethod());
            result = methodRef.get();
        }
        return result;
    }

    private Method tryFindMethod() {
        Class<?> type = pluginClass.tryGetClass();
        if (type == null) {
            return null;
        }

        try {
            return type.getMethod(methodName, getArgTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public Object tryInvoke(Object instance, Object... arguments) {
        Method method = tryGetMethod();
        try {
            return method != null
                    ? method.invoke(instance, arguments)
                    : null;
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
