package org.netbeans.gradle.project.others;

import java.util.concurrent.atomic.AtomicReference;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/**
 *
 * @author Kelemen Attila
 */
public final class PluginClass {
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

    public Class<?> tryGetClass() {
        Class<?> result = loadedClass.get();
        if (result == null) {
            loadedClass.compareAndSet(null, classFactory.tryFindClass(className));
            result = loadedClass.get();
        }
        return result;
    }
}
