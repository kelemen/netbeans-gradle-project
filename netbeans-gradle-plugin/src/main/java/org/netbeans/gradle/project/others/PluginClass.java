package org.netbeans.gradle.project.others;

import java.util.concurrent.atomic.AtomicReference;

public final class PluginClass implements ClassFinder {
    private final PluginClassFactory classFactory;
    private final String className;
    private final AtomicReference<Class<?>> loadedClass;

    public PluginClass(PluginClassFactory classFactory, String className) {
        if (classFactory == null) throw new NullPointerException("classFactory");
        if (className == null) throw new NullPointerException("className");

        this.classFactory = classFactory;
        this.className = className;
        this.loadedClass = new AtomicReference<Class<?>>();
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
