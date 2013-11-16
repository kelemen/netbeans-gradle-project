package org.netbeans.gradle.project.api.entry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.util.CollectionUtils;

/**
 * Defines the information need from the Gradle daemon by an extension of the
 * Gradle plugin of NetBeans.
 * <P>
 * Currently, you can request the following information:
 * <ul>
 *  <li>Simple models, such as {@link org.gradle.tooling.model.idea.IdeaProject}</li>
 *  <li>Queries which can extract any information given a {@link org.gradle.api.Project} instance.</li>
 * </ul>
 * <P>
 * <B>Warning</B>: There are some known problems in requesting custom information
 * via a {@link GradleProjectInfoQuery}. The limitation is that implementations
 * of {@link org.netbeans.gradle.model.api.ProjectInfoBuilder} must be provided
 * by the Gradle plugin itself.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently.
 *
 * TODO: Reference the new queries.
 */
public final class GradleModelDef {
    /**
     * An instance of {@code GradleModelDef} requesting no models.
     */
    public static final GradleModelDef EMPTY = new GradleModelDef(
            Collections.<Class<?>>emptySet(),
            Collections.<GradleProjectInfoQuery<?>>emptySet());

    private final Collection<Class<?>> toolingModels;
    private final Collection<GradleProjectInfoQuery<?>> projectInfoQueries;

    /**
     * Creates a new {@code GradleModelDef} with the given requested information.
     *
     * @param toolingModels the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @param projectInfoQueries custom queries able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     */
    public GradleModelDef(
            @Nonnull Collection<? extends Class<?>> toolingModels,
            @Nonnull Collection<? extends GradleProjectInfoQuery<?>> projectInfoQueries) {
        this.toolingModels = CollectionUtils.copyNullSafeList(toolingModels);
        this.projectInfoQueries = CollectionUtils.copyNullSafeList(projectInfoQueries);
    }

    /**
     * Creates a new {@code GradleModelDef} with the given Tooling API models
     * and with no {@link #getProjectInfoQueries() custom queries}.
     *
     * @param modelTypes the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given Tooling API models.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public static GradleModelDef fromToolingModels(Class<?>... modelTypes) {
        return new GradleModelDef(
                Arrays.asList(modelTypes),
                Collections.<GradleProjectInfoQuery<?>>emptyList());
    }

    /**
     * Creates a new {@code GradleModelDef} with the given custom queries and
     * with no {@link #getToolingModels() Tooling API models}.
     *
     * @param queries custom queries able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given custom queries.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public static GradleModelDef fromProjectQueries(GradleProjectInfoQuery<?>... queries) {
        return new GradleModelDef(Collections.<Class<?>>emptyList(), Arrays.asList(queries));
    }

    /**
     * Returns the requested simple Tooling API models. Examples of such models
     * are: {@link org.gradle.tooling.model.GradleProject},
     * {@link org.gradle.tooling.model.idea.IdeaProject}.
     *
     * @return the requested simple Tooling API models. This method never
     *   returns {@code null} and the returned collection does not contain
     *   {@code null} elements.
     */
    @Nonnull
    public Collection<Class<?>> getToolingModels() {
        return toolingModels;
    }

    /**
     * Returns the custom queries the retrieve information from Gradle projects
     * from a {@link org.gradle.api.Project} instance.
     *
     * @return the custom queries the retrieve information from Gradle projects.
     *   This method never returns {@code null} and the returned collection does
     *   not contain {@code null} elements.
     */
    @Nonnull
    public Collection<GradleProjectInfoQuery<?>> getProjectInfoQueries() {
        return projectInfoQueries;
    }
}
