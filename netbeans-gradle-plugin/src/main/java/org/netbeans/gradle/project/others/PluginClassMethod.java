package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

public final class PluginClassMethod {
    private static final Logger LOGGER = Logger.getLogger(PluginClassMethod.class.getName());
    private static final Class<?>[] EMPTY_CLASS_ARR = new Class<?>[0];

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
        ExceptionHelper.checkNotNullArgument(pluginClass, "pluginClass");
        ExceptionHelper.checkNotNullArgument(methodName, "methodName");

        this.pluginClass = pluginClass;
        this.methodName = methodName;
        this.argTypeFinders = argTypeFinders.clone();
        this.methodRef = new AtomicReference<>(null);
        this.argTypesCache = null;

        ExceptionHelper.checkNotNullElements(this.argTypeFinders, "argTypeFinders");
    }

    public static PluginClassMethod noArgMethod(ClassFinder pluginClass, String methodName) {
        return new PluginClassMethod(pluginClass, methodName, EMPTY_CLASS_ARR);
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

    private String getClassName() {
        Class<?> type = pluginClass.tryGetClass();
        return type != null ? type.getName() : "?";
    }

    public Object tryInvoke(Object instance, Object... arguments) {
        Method method = tryGetMethod();
        try {
            if (method != null) {
                return method.invoke(instance, arguments);
            }
            else {
                LOGGER.log(Level.WARNING,
                        "Missing method: {0} for class {1}",
                        new Object[]{methodName, getClassName()});
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
