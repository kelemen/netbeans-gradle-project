package org.netbeans.gradle.project.others;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public final class PluginClassConstructor {
    private final ClassFinder pluginClass;
    private final Class<?>[] argTypes;
    private final AtomicReference<Constructor<?>> constructorRef;

    public PluginClassConstructor(ClassFinder pluginClass, Class<?>[] argTypes) {
        if (pluginClass == null) throw new NullPointerException("pluginClass");

        this.pluginClass = pluginClass;
        this.argTypes = argTypes.clone();
        this.constructorRef = new AtomicReference<Constructor<?>>(null);
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
            return type.getConstructor(argTypes);
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
