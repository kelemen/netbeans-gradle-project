package org.netbeans.gradle.project.others;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class PluginClass implements ClassFinder {
    private final PluginClassFactory classFactory;
    private final String className;
    private final AtomicReference<Class<?>> loadedClass;

    public PluginClass(PluginClassFactory classFactory, String className) {
        this.classFactory = Objects.requireNonNull(classFactory, "classFactory");
        this.className = Objects.requireNonNull(className, "className");
        this.loadedClass = new AtomicReference<>();
    }

    @Override
    public Class<?> tryGetClass() {
        Class<?> result = loadedClass.get();
        if (result == null) {
            loadedClass.compareAndSet(null, classFactory.tryFindClass(className));
            result = loadedClass.get();
        }
        return result;
    }
}
