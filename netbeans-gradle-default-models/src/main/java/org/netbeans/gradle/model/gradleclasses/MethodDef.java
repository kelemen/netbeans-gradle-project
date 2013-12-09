package org.netbeans.gradle.model.gradleclasses;

import java.lang.reflect.Method;
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
}
