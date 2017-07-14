package org.netbeans.gradle.project.others;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.LazyValues;

public final class PluginClassConstructor {
    private final ClassFinder pluginClass;
    private final ClassFinder[] argTypeFinders;
    private final Supplier<Constructor<?>> constructorRef;

    public PluginClassConstructor(ClassFinder pluginClass, Class<?>... argTypes) {
        this(pluginClass, ReflectionHelper.constClassFinders(argTypes));
    }

    public PluginClassConstructor(ClassFinder pluginClass, ClassFinder... argTypeFinders) {
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
        this.argTypeFinders = argTypeFinders.clone();
        this.constructorRef = LazyValues.lazyValue(this::tryFindConstructor);

        ExceptionHelper.checkNotNullElements(this.argTypeFinders, "argTypeFinders");
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

    public Constructor<?> tryGetConstructor() {
        return constructorRef.get();
    }

    private Constructor<?> tryFindConstructor() {
        Class<?> type = pluginClass.tryGetClass();
        if (type == null) {
            return null;
        }

        try {
            return type.getConstructor(findArgTypes());
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
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
