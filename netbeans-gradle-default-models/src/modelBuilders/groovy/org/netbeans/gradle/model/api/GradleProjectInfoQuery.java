package org.netbeans.gradle.model.api;

/**
 * @deprecated Use {@link GradleProjectInfoQuery2} instead. This interface prevents
 *   the usage of newer Tooling API versions, so it will be removed for the next
 *   major release of NetBeans.
 * <P>
 * Defines the query to be submitted for the Gradle daemon to query information
 * from the project.
 *
 * @param <T> the type of the object extracted from the project. Note that this
 *   type must be serializable in order to be able to transfer the extracted
 *   information from the Gradle daemon to the caller.
 *
 * @see ProjectInfoBuilder
 */
@Deprecated
public interface GradleProjectInfoQuery<T> extends GradleInfoQuery {
    /**
     * Returns the builder which will extract the information from the project
     * object. The builder will be serialized and be executed in the context
     * of the Gradle daemon evaluating the project.
     *
     * @return the builder which will extract the information from the project
     *   object. This method may never return {@code null}.
     */
    public ProjectInfoBuilder<T> getInfoBuilder();
}
