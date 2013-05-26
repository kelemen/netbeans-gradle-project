package org.netbeans.gradle.project.others;

import java.util.concurrent.atomic.AtomicReference;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/**
 *
 * @author Kelemen Attila
 */
public final class PluginClass {
    private final String pluginName;
    private final String className;
    private final AtomicReference<Class<?>> loadedClass;

    public PluginClass(String pluginName, String className) {
        this.pluginName = pluginName;
        this.className = className;
        this.loadedClass = new AtomicReference<Class<?>>();
    }

    private Class<?> tryFindClass() {
        try {
            for (ModuleInfo info: Lookup.getDefault().lookupAll(ModuleInfo.class)) {
                String codeName = info.getCodeName();
                if (codeName != null && codeName.startsWith(pluginName)) {
                    return Class.forName(className, true, info.getClassLoader());
                }
            }
        } catch (ClassNotFoundException ex) {
        }

        return null;
    }

    public Class<?> tryGetClass() {
        Class<?> result = loadedClass.get();
        if (result == null) {
            loadedClass.compareAndSet(null, tryFindClass());
            result = loadedClass.get();
        }
        return result;
    }
}
