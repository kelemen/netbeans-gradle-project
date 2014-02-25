package org.netbeans.gradle.project.api.modelquery;

import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Defines a query which returns the required models for a given extension.
 * Extensions must provide an instance of this query on the lookup returned
 * by the {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#getLookup()}
 * if they want to support the earlier (1.7 or below) versions of Gradle. Also
 * note that users can configure NetBeans to rely on Gradle 1.7 API for newer
 * Gradle versions as well. This means, that if you don't provide this query,
 * your extension will be loaded with an empty {@code Lookup} which usually
 * means that it will be disabled.
 * <P>
 * If you need more detailed information provide an instance of
 * {@link GradleModelDefQuery2} as well. If {@code GradleModelDefQuery2} is
 * found on the lookup and can be used for the target Gradle version,
 * {@code GradleModelDefQuery1} is ignored.
 *
 * @see org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef
 * @see GradleModelDefQuery2
 */
public interface GradleModelDefQuery1 {
    /**
     * Returns the models to be requested through the Tooling API of Gradle.
     * <P>
     * The retrieved models will be passed to the
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#parseModel(ModelLoadResult) parseModel}
     * method of {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef}.
     *
     * @param gradleTarget the target Gradle environment used to evaluate the
     *   build scripts. This argument cannot be {@code null}.
     * @return the models to be requested through the Tooling API of Gradle.
     *   This method may never return {@code null} and elements of the returned
     *   collection cannot be {@code null}.
     */
    @Nonnull
    public Collection<Class<?>> getToolingModels(@Nonnull GradleTarget gradleTarget);
}
