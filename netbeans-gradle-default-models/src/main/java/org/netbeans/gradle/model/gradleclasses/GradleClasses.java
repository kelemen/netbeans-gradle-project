package org.netbeans.gradle.model.gradleclasses;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.gradle.api.Project;

public final class GradleClasses {
    private static final ConcurrentMap<String, GradleClass> CLASSES_CACHE
            = new ConcurrentHashMap<String, GradleClass>();

    public static GradleClass getGradleClass(Project project, String className) throws ClassNotFoundException {
        GradleClass cachedClass = CLASSES_CACHE.get(className);
        GradleClass result = cachedClass != null
                ? cachedClass.getGradleClass(project, className)
                : new GradleClass(project, className);

        if (result != cachedClass) {
            CLASSES_CACHE.put(className, result);
        }
        return result;
    }

    public static <T> Class<? extends T> tryGetGradleClass(Project project, String className, Class<T> subType) {
        Class<?> result = tryGetGradleClass(project, className);
        if (subType.isAssignableFrom(result)) {
            @SuppressWarnings("unchecked")
            Class<? extends T> typeSafeResult = (Class<? extends T>)result;
            return typeSafeResult;
        }
        else {
            return null;
        }
    }

    public static Class<?> tryGetGradleClass(Project project, String className) {
        try {
            GradleClass gradleClass = getGradleClass(project, className);
            return gradleClass.getType();
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private GradleClasses() {
        throw new AssertionError();
    }
}
