package org.netbeans.gradle.project.others;

import java.lang.reflect.Method;
import org.openide.util.Lookup;

public final class PluginLookupMethod {
    private final PluginClass pluginClass;
    private final String methodName;
    private final Class<?>[] argTypes;

    public PluginLookupMethod(PluginClass pluginClass, String methodName, Class<?>... argTypes) {
        this.pluginClass = pluginClass;
        this.methodName = methodName;
        this.argTypes = argTypes.clone();
    }

    public Object tryCall(Object... args) {
        Class<?> cl = pluginClass.tryGetClass();
        if (cl == null) {
            return null;
        }

        Object obj = Lookup.getDefault().lookup(cl);
        if (obj == null) {
            return null;
        }

        Method method = ReflectionHelper.tryGetMethod(cl, methodName, argTypes);
        if (method == null) {
            return null;
        }

        return ReflectionHelper.tryInvoke(method, obj, args);
    }
}
