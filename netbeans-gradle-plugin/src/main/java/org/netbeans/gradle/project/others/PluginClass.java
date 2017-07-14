package org.netbeans.gradle.project.others;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;

public final class PluginClass implements ClassFinder {
    private final Supplier<Class<?>> loadedClass;

    public PluginClass(PluginClassFactory classFactory, String className) {
        Objects.requireNonNull(classFactory, "classFactory");
        Objects.requireNonNull(className, "className");

        this.loadedClass = LazyValues.lazyValue(() -> classFactory.tryFindClass(className));
    }

    @Override
    public Class<?> tryGetClass() {
        return loadedClass.get();
    }
}
