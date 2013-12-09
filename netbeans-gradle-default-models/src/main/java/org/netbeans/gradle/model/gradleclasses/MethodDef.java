package org.netbeans.gradle.model.gradleclasses;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.netbeans.gradle.model.util.ReflectionUtils;

public final class MethodDef {
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private final String name;
    private final Class<?>[] parameterTypes;

    public MethodDef(String name, Class<?>... parameterTypes) {
        this.name = name;
        this.parameterTypes = parameterTypes.length > 0
                ? parameterTypes.clone()
                : EMPTY_CLASS_ARRAY;
    }

    public Method findForType(Class<?> type) throws NoSuchMethodException {
        return ReflectionUtils.getAccessibleMethod(type, name, parameterTypes);
    }

    public String getName() {
        return name;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + name.hashCode();
        hash = 61 * hash + Arrays.hashCode(parameterTypes);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final MethodDef other = (MethodDef)obj;
        return this.name.equals(other.name) && Arrays.equals(this.parameterTypes, other.parameterTypes);
    }
}
