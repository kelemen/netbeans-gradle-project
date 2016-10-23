package org.netbeans.gradle.model.api.util;

import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.internal.ConstrProjectInfoBuilderRef;
import org.netbeans.gradle.model.internal.EnumProjectInfoBuilderRef;

/**
 * Defines convenience factory methods to wrap {@link ProjectInfoBuilder2} instances and create
 * them via reflection. Note that if you directly access Gradle API, you must use reflection to create an
 * instance of your {@code ProjectInfoBuilder2} because the Gradle API is only available when the model
 * is actually being loaded.
 * <P>
 * The convenience methods of {@code GradleModelDef} are aware of wrappers created by these methods and will
 * find the appropriate class loader and class path through these wrappers.
 */
public final class ModelBuilders {

    /**
     * Returns a wrapper of an {@code enum} based implementation of {@code ProjectInfoBuilder2} and creates it via
     * reflection.
     * <P>
     * The class loader of the specified model type will be used to load the wrapped builder.
     *
     * @param <T> the type of the model created by the wrapped {@code ProjectInfoBuilder2}
     * @param modelType the type of the model created by the wrapped {@code ProjectInfoBuilder2}. This argument
     *   cannot be {@code null}.
     * @param wrappedTypeName the name of the class implementing {@code ProjectInfoBuilder2}. The type referenced
     *   by this argument must also be an {@code enum} with exactly one enum constant (its name is not relevant).
     *   If this name does not contain a package definition, the builder is expected to be in the same package
     *   as the model it returns. This argument cannot be {@code null}.
     * @return {@code ProjectInfoBuilder2} wrapping the specified {@code ProjectInfoBuilder2} and creating
     *   it lazily via reflection. This method never returns {@code null}.
     */
    public static <T> ProjectInfoBuilder2<T> wrapEnumBuilder(
            Class<? extends T> modelType,
            String wrappedTypeName) {
        return new EnumProjectInfoBuilderRef<T>(modelType, wrappedTypeName);
    }

    /**
     * Returns a wrapper of an {@code enum} based implementation of {@code ProjectInfoBuilder2} and creates it via
     * reflection.
     * <P>
     * The class loader of the specified model type will be used to load the wrapped builder.
     *
     * @param <T> the type of the model created by the wrapped {@code ProjectInfoBuilder2}
     * @param modelType the type of the model created by the wrapped {@code ProjectInfoBuilder2}. This argument
     *   cannot be {@code null}.
     * @param wrappedTypeName the name of the class implementing {@code ProjectInfoBuilder2}. The type referenced
     *   by this argument must also be an {@code enum}. If this name does not contain a package definition,
     *   the builder is expected to be in the same package as the model it returns. This argument
     *   cannot be {@code null}.
     * @param wrappedConstName the name of the enum constant used by the returned wrapper. This argument
     *   cannot be {@code null}.
     * @return {@code ProjectInfoBuilder2} wrapping the specified {@code ProjectInfoBuilder2} and creating
     *   it lazily via reflection. This method never returns {@code null}.
     */
    public static <T> ProjectInfoBuilder2<T> wrapEnumBuilder(
            Class<? extends T> modelType,
            String wrappedTypeName,
            String wrappedConstName) {
        return new EnumProjectInfoBuilderRef<T>(modelType, wrappedTypeName, wrappedConstName);
    }

    /**
     * Returns a wrapper of a {@code ProjectInfoBuilder2} and creates it via reflection using an appropriate
     * constructor.
     * <P>
     * <B>Warning</B>: The specified arguments must be serializable and deserializable using the
     * class loader of the model.
     * <P>
     * The class loader of the specified model type will be used to load the wrapped builder.
     *
     * @param <T> the type of the model created by the wrapped {@code ProjectInfoBuilder2}
     * @param modelType the type of the model created by the wrapped {@code ProjectInfoBuilder2}. This argument
     *   cannot be {@code null}.
     * @param wrappedTypeName the name of the class implementing {@code ProjectInfoBuilder2}. If this name
     *   does not contain a package definition,
     *   the builder is expected to be in the same package as the model it returns. This argument
     *   cannot be {@code null}.
     * @param arguments the argument passed to the appropriate constructor. If there are multiple appropriate
     *   constructors, it is undefined which one is chosen. All the arguments must be serializable. The passed
     *   array cannot be {@code null} but any of its element might be {@code null}.
     * @return {@code ProjectInfoBuilder2} wrapping the specified {@code ProjectInfoBuilder2} and creating
     *   it lazily via reflection. This method never returns {@code null}.
     */
    public static <T> ProjectInfoBuilder2<T> wrapWithConstructor(
            Class<? extends T> modelType,
            String wrappedTypeName,
            Object... arguments) {
        return new ConstrProjectInfoBuilderRef<T>(modelType, wrappedTypeName, arguments);
    }

    private ModelBuilders() {
        throw new AssertionError();
    }
}
