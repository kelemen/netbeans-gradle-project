package org.netbeans.gradle.model.gradleclasses;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.gradle.api.Project;
import org.netbeans.gradle.model.util.ClassLoaderUtils;

public final class GradleClass {
    private final Class<?> type;
    private final ConcurrentMap<MethodDef, Method> methods;

    public GradleClass(Project project, String className) throws ClassNotFoundException {
        this(ClassLoaderUtils.getClass(project, className));
    }

    private GradleClass(Class<?> type) {
        this.type = type;
        this.methods = new ConcurrentHashMap<MethodDef, Method>();
    }

    public GradleClass getGradleClass(Project project, String className) throws ClassNotFoundException {
        Class<?> currentType = ClassLoaderUtils.getClass(project, className);
        if (currentType == type) {
            return this;
        }

        return new GradleClass(type);
    }

    public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getMethod(new MethodDef(name, parameterTypes));
    }

    public Method getMethod(MethodDef methodDef) throws NoSuchMethodException {
        Method result = methods.get(methodDef);
        if (result == null) {
            result = methodDef.findForType(type);
            Method prevValue = methods.putIfAbsent(methodDef, result);
            if (prevValue != null) {
                result = prevValue;
            }
        }
        return result;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return 291 + type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final GradleClass other = (GradleClass)obj;
        return this.type == other.type;
    }
}
