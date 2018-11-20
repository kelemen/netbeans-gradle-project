package org.netbeans.gradle.model.internal;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.ReflectionUtils;

public final class ConstrProjectInfoBuilderRef<T> implements ProjectInfoBuilder2<T>, BuilderWrapper {
    private static final long serialVersionUID = 1L;

    private final Class<? extends T> modelType;
    private final String wrappedTypeName;
    private final Object[] arguments;

    private final AtomicReference<ProjectInfoBuilder2<?>> wrappedRef;

    public ConstrProjectInfoBuilderRef(Class<? extends T> modelType, String wrappedTypeName, Object[] arguments) {
        if (modelType == null) throw new NullPointerException("modelType");
        if (wrappedTypeName == null) throw new NullPointerException("wrappedTypeName");

        this.modelType = modelType;
        this.wrappedTypeName = ReflectionUtils.updateTypeName(modelType, wrappedTypeName);
        this.arguments = arguments.clone();
        this.wrappedRef = new AtomicReference<ProjectInfoBuilder2<?>>(null);
    }

    private static boolean isApplicable(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = arguments[i];
            if (arg != null && !parameterTypes[i].isInstance(arg)) {
                return false;
            }
        }

        return true;
    }

    private RuntimeException rethrow(Throwable ex) {
        if (ex instanceof InvocationTargetException) {
            return rethrow(ex.getCause());
        }

        if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }
        if (ex instanceof Error) {
            throw (Error)ex;
        }
        throw new RuntimeException(ex);
    }

    private ProjectInfoBuilder2<?> getWrapped() {
        ProjectInfoBuilder2<?> result = wrappedRef.get();
        if (result == null) {
            result = createWrapped();
            if (!wrappedRef.compareAndSet(null, result)) {
                result = wrappedRef.get();
            }
        }
        return result;
    }

    private ProjectInfoBuilder2<?> createWrapped() {
        try {
            Class<?> wrappedType = Class.forName(wrappedTypeName, false, modelType.getClassLoader());
            for (Constructor<?> constr: wrappedType.getConstructors()) {
                if (isApplicable(constr.getParameterTypes(), arguments)) {
                    return (ProjectInfoBuilder2<?>)constr.newInstance(arguments);
                }
            }
        } catch (Exception ex) {
            throw rethrow(ex);
        }

        throw new IllegalStateException("Cannot find appropriate constructor for " + wrappedTypeName);
    }

    @Override
    public T getProjectInfo(Object project) {
        Object result = getWrapped().getProjectInfo(project);
        return modelType.cast(result);
    }

    @Override
    public String getName() {
        return getWrapped().getName();
    }

    @Override
    public Object getWrappedObject() {
        return null;
    }

    @Override
    public Class<?> getWrappedType() {
        return modelType;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> modelType;
        private final String wrappedTypeName;
        private final Object[] arguments;

        public SerializedFormat(ConstrProjectInfoBuilderRef<?> source) {
            this.modelType = source.modelType;
            this.wrappedTypeName = source.wrappedTypeName;
            this.arguments = source.arguments;
        }

        private Object readResolve() throws ObjectStreamException {
            return new ConstrProjectInfoBuilderRef<Object>(modelType, wrappedTypeName, arguments);
        }
    }
}
