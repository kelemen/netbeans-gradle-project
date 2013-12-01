package org.netbeans.gradle.model.util;

public final class BuilderUtils {
    public static String getNameForEnumBuilder(Enum<?> instance) {
        return instance.getClass().getSimpleName() + '.' + instance.name();
    }

    public static String getNameForGenericBuilder(Object instance, String args) {
        return instance.getClass().getSimpleName() + '(' + args + ')';
    }

    private BuilderUtils() {
        throw new AssertionError();
    }
}
