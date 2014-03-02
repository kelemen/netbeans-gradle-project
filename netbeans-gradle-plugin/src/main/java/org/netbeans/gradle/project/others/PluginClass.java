package org.netbeans.gradle.project.others;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class PluginClass implements ClassFinder {
    private final PluginClassFactory classFactory;
    private final String className;
    private final AtomicReference<Class<?>> loadedClass;

    public PluginClass(PluginClassFactory classFactory, String className) {
        ExceptionHelper.checkNotNullArgument(classFactory, "classFactory");
        ExceptionHelper.checkNotNullArgument(className, "className");

        this.classFactory = classFactory;
        this.className = className;
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
