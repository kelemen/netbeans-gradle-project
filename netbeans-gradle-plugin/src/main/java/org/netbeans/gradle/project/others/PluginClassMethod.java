package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.LazyValues;

public final class PluginClassMethod {
    private static final Logger LOGGER = Logger.getLogger(PluginClassMethod.class.getName());
    private static final Class<?>[] EMPTY_CLASS_ARR = new Class<?>[0];

    private final ClassFinder pluginClass;
    private final String methodName;
    private final ClassFinder[] argTypeFinders;
    private final Supplier<Method> methodRef;

    @SuppressWarnings("VolatileArrayField")
    private volatile Class<?>[] argTypesCache;

    public PluginClassMethod(ClassFinder pluginClass, String methodName, Class<?>... argTypes) {
        this(pluginClass, methodName, ReflectionHelper.constClassFinders(argTypes));
    }

    public PluginClassMethod(ClassFinder pluginClass, String methodName, ClassFinder... argTypeFinders) {
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
        this.methodName = Objects.requireNonNull(methodName, "methodName");
        this.argTypeFinders = argTypeFinders.clone();
        this.methodRef = LazyValues.lazyValue(this::tryFindMethod);
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
        return methodRef.get();
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
