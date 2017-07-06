package org.netbeans.gradle.project.others;

import java.util.Objects;

public final class PluginEnum implements ClassFinder {
    private final ClassFinder pluginClass;

    public PluginEnum(ClassFinder pluginClass) {
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
    }

    public PluginEnum(PluginClassFactory classFactory, String className) {
        this(new PluginClass(classFactory, className));
    }

    @Override
    public Class<?> tryGetClass() {
        return pluginClass.tryGetClass();
    }

    public Object tryGetEnumConst(String name) {
        Objects.requireNonNull(name, "name");

        Class<?> type = pluginClass.tryGetClass();
        if (type == null) {
            return null;
        }

        Object[] enumTypes = type.getEnumConstants();
        if (enumTypes == null) {
            return null;
        }

        for (Object enumType: enumTypes) {
            if (enumType instanceof Enum) {
                if (name.equals(((Enum<?>)enumType).name())) {
                    return enumType;
                }
            }
        }

        return null;
    }
}
