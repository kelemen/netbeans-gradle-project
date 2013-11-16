package org.netbeans.gradle.model;

import org.netbeans.gradle.model.api.GradleInfoQuery;

/**
 * Defines the query to be submitted for the Gradle daemon to query information
 * from the Gradle build.
 * <P>
 * Note that the Tooling API always requires a project to be associated with
 * the query. However, it is not necessarily evaluated, unless accessed directly
 * or indirectly.
 *
 * @param <T> the type of the object extracted from the Gradle build. Note that
 *   this type must be serializable in order to be able to transfer the
 *   extracted information from the Gradle daemon to the caller.
 *
 * @see ProjectInfoBuilder
 */
public interface GradleBuildInfoQuery<T> extends GradleInfoQuery {
    /**
     * Returns the builder which will extract the information from the Gradle
     * build. The builder will be serialized and be executed in the context
     * of the Gradle daemon evaluating the associated project.
     *
     * @return the builder which will extract the information from the project
     *   object. This method may never return {@code null}.
     */
    public BuildInfoBuilder<T> getInfoBuilder();
}
