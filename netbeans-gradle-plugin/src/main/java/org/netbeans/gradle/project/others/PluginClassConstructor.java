package org.netbeans.gradle.project.others;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class PluginClassConstructor {
    private final ClassFinder pluginClass;
    private final ClassFinder[] argTypeFinders;
    private final AtomicReference<Constructor<?>> constructorRef;

    @SuppressWarnings("VolatileArrayField")
    private volatile Class<?>[] argTypesCache;

    public PluginClassConstructor(ClassFinder pluginClass, Class<?>... argTypes) {
        this(pluginClass, ReflectionHelper.constClassFinders(argTypes));
    }

    public PluginClassConstructor(ClassFinder pluginClass, ClassFinder... argTypeFinders) {
        if (pluginClass == null) throw new NullPointerException("pluginClass");

        this.pluginClass = pluginClass;
        this.argTypeFinders = argTypeFinders.clone();
        this.constructorRef = new AtomicReference<Constructor<?>>(null);
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

    public Constructor<?> tryGetConstructor() {
        Constructor<?> result = constructorRef.get();
        if (result == null) {
            constructorRef.compareAndSet(null, tryFindConstructor());
            result = constructorRef.get();
        }
        return result;
    }

    private Constructor<?> tryFindConstructor() {
        Class<?> type = pluginClass.tryGetClass();
        if (type == null) {
            return null;
        }

        try {
            return type.getConstructor(getArgTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public Object tryCreateInstance(Object... arguments) {
        Constructor<?> constructor = tryGetConstructor();
        try {
            return constructor != null
                    ? constructor.newInstance(arguments)
                    : null;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
