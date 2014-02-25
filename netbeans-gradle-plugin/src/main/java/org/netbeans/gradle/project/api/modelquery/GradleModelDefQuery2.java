package org.netbeans.gradle.project.api.modelquery;

import javax.annotation.Nonnull;

/**
 * Defines a query returning the required information from build scripts of
 * Gradle projects. Extensions may provide an instance of this query on the lookup returned
 * by the {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#getLookup()}
 * method.
 * <P>
 * Note that this query is used for Gradle 1.8 or later, if you need to support
 * previous versions of Gradle (or consider the fact that NetBeans users may
 * disable using Gradle 1.8 API), you have to implement {@link GradleModelDefQuery1}
 * as well. Also, it is fine to only implement {@link GradleModelDefQuery1} if
 * you don't need the features of this query.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef
 * @see GradleModelDefQuery2
 */
public interface GradleModelDefQuery2 {
    /**
     * Returns the {@code GradleModelDef} instance defining the information
     * to be retrieved from build scripts of Gradle projects.
     * <P>
     * The retrieved models will be passed to the
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#parseModel(ModelLoadResult) parseModel}
     * method of {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef}.
     *
     * @param gradleTarget the target Gradle environment used to evaluate the
     *   build scripts. This argument cannot be {@code null}.
     * @return the {@code GradleModelDef} instance defining the information
     *   to be retrieved from build scripts of Gradle projects. This method
     *   may never return {@code null}.
     */
    @Nonnull
    public GradleModelDef getModelDef(@Nonnull GradleTarget gradleTarget);
}
