package org.netbeans.gradle.model.internal;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.ReflectionUtils;

public final class EnumProjectInfoBuilderRef<T> implements ProjectInfoBuilder2<T>, BuilderWrapper {
    private static final long serialVersionUID = 1L;

    private final Class<? extends T> modelType;
    private final String wrappedTypeName;
    private final String wrappedConstName;

    private final AtomicReference<ProjectInfoBuilder2<?>> wrappedRef;

    public EnumProjectInfoBuilderRef(
            Class<? extends T> modelType,
            String wrappedTypeName) {
        if (modelType == null) throw new NullPointerException("modelType");
        if (wrappedTypeName == null) throw new NullPointerException("wrappedTypeName");

        this.modelType = modelType;
        this.wrappedTypeName = ReflectionUtils.updateTypeName(modelType, wrappedTypeName);
        this.wrappedConstName = null;
        this.wrappedRef = new AtomicReference<ProjectInfoBuilder2<?>>(null);
    }

    public EnumProjectInfoBuilderRef(
            Class<? extends T> modelType,
            String wrappedTypeName,
            String wrappedConstName) {
        if (modelType == null) throw new NullPointerException("modelType");
        if (wrappedTypeName == null) throw new NullPointerException("wrappedTypeName");
        if (wrappedConstName == null) throw new NullPointerException("wrappedConstName");

        this.modelType = modelType;
        this.wrappedTypeName = ReflectionUtils.updateTypeName(modelType, wrappedTypeName);
        this.wrappedConstName = wrappedConstName;
        this.wrappedRef = new AtomicReference<ProjectInfoBuilder2<?>>(null);
    }

    private static Object unsafeEnumValueOf(Class<?> type, String constName) {
        Object[] enumConsts = type.getEnumConstants();
        if (constName != null) {
            for (Object enumConst: enumConsts) {
                String name = ((Enum<?>)enumConst).name();
                if (constName.equals(name)) {
                    return enumConst;
                }
            }
            throw new IllegalStateException("No such enum constant for type " + type.getName() + ": " + constName);
        }
        else {
            if (enumConsts.length != 1) {
                throw new IllegalStateException("Cannot determine which enum const must be used: " + Arrays.asList(enumConsts));
            }
            return enumConsts[0];
        }
    }

    private ProjectInfoBuilder2<?> createWrapped() {
        try {
            return (ProjectInfoBuilder2<?>)unsafeEnumValueOf(
                    Class.forName(wrappedTypeName, false, modelType.getClassLoader()),
                    wrappedConstName);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            throw new RuntimeException(ex);
        }
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
        private final String wrappedConstName;

        public SerializedFormat(EnumProjectInfoBuilderRef<?> source) {
            this.modelType = source.modelType;
            this.wrappedTypeName = source.wrappedTypeName;
            this.wrappedConstName = source.wrappedConstName;
        }

        private Object readResolve() throws ObjectStreamException {
            return wrappedConstName != null
                    ? new EnumProjectInfoBuilderRef<Object>(modelType, wrappedTypeName, wrappedConstName)
                    : new EnumProjectInfoBuilderRef<Object>(modelType, wrappedTypeName);
        }
    }
}
