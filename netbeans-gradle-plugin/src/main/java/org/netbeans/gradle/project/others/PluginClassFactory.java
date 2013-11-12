package org.netbeans.gradle.project.others;

import java.util.concurrent.atomic.AtomicReference;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

public final class PluginClassFactory {
    private final String moduleNamePrefix;
    private final AtomicReference<ModuleInfo> moduleInfoRef;

    public PluginClassFactory(String moduleNamePrefix) {
        if (moduleNamePrefix == null) throw new NullPointerException("moduleNamePrefix");

        this.moduleNamePrefix = moduleNamePrefix;
        this.moduleInfoRef = new AtomicReference<ModuleInfo>(null);
    }

    private ModuleInfo tryFindModuleInfo() {
        for (ModuleInfo info: Lookup.getDefault().lookupAll(ModuleInfo.class)) {
            String codeName = info.getCodeName();
            if (codeName != null && codeName.startsWith(moduleNamePrefix)) {
                return info;
            }
        }

        return null;
    }

    private ModuleInfo tryGetModuleInfo() {
        ModuleInfo result = moduleInfoRef.get();
        if (result == null) {
            moduleInfoRef.compareAndSet(null, tryFindModuleInfo());
            result = moduleInfoRef.get();
        }
        return result;
    }

    public ClassLoader tryGetModuleClassLoader() {
        ModuleInfo moduleInfo = tryGetModuleInfo();
        return moduleInfo != null ? moduleInfo.getClassLoader() : null;
    }

    public Class<?> tryFindClass(String className) {
        try {
            ClassLoader classLoader = tryGetModuleClassLoader();
            if (classLoader != null) {
                return Class.forName(className, true, classLoader);
            }
        } catch (ClassNotFoundException ex) {
        }

        return null;
    }
}