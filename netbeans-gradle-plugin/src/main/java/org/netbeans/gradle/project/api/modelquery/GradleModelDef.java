package org.netbeans.gradle.project.api.modelquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.CompatibilityUtils;

/**
 * Defines the information needed from the Gradle daemon by an extension of the
 * Gradle plugin of NetBeans.
 * <P>
 * Currently, you can request the following information:
 * <ul>
 *  <li>Simple models, such as {@link org.gradle.tooling.model.idea.IdeaProject}</li>
 *  <li>Queries which can extract any information given a {@link org.gradle.api.Project} instance.</li>
 * </ul>
 * <P>
 * Note: In almost all cases, it is recommended to use one of the convenience factory
 * method: {@link #fromProjectInfoBuilders(ProjectInfoBuilder[]) fromProjectInfoBuilders}.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently.
 *
 * @see GradleModelDefQuery2
 */
public final class GradleModelDef {
    /**
     * An instance of {@code GradleModelDef} requesting no models.
     */
    public static final GradleModelDef EMPTY = create(
            Collections.<Class<?>>emptySet(),
            Collections.<GradleProjectInfoQuery2<?>>emptySet());

    private final Collection<Class<?>> toolingModels;
    private final Collection<GradleProjectInfoQuery2<?>> projectInfoQueries;

    /**
     * @deprecated Use {@link #create} instead because
     *   {@code GradleProjectInfoQuery} is deprecated.
     * <P>
     * Creates a new {@code GradleModelDef} with the given requested information.
     *
     * @param toolingModels the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @param projectInfoQueries custom queries able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     */
    @Deprecated
    public GradleModelDef(
            @Nonnull Collection<? extends Class<?>> toolingModels,
            @Nonnull Collection<? extends org.netbeans.gradle.model.api.GradleProjectInfoQuery<?>> projectInfoQueries) {
        this(toolingModels, CompatibilityUtils.toQuery2All(projectInfoQueries), false);
    }

    private GradleModelDef(
            @Nonnull Collection<? extends Class<?>> toolingModels,
            @Nonnull Collection<? extends GradleProjectInfoQuery2<?>> projectInfoQueries,
            boolean x) {
        // argument x is only used because we need to overload the constructor
        this.toolingModels = CollectionUtils.copyNullSafeList(toolingModels);
        this.projectInfoQueries = CollectionUtils.copyNullSafeList(projectInfoQueries);
    }

    /**
     * Creates a new {@code GradleModelDef} with the given requested information.
     *
     * @param toolingModels the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @param projectInfoQueries custom queries able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given requested information.
     *   This method never returns {@code null}.
     */
    public static GradleModelDef create(
            @Nonnull Collection<? extends Class<?>> toolingModels,
            @Nonnull Collection<? extends GradleProjectInfoQuery2<?>> projectInfoQueries) {
        return new GradleModelDef(toolingModels, projectInfoQueries, true);
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
        return create(
                Arrays.asList(modelTypes),
                Collections.<GradleProjectInfoQuery2<?>>emptyList());
    }

    /**
     * @deprecated Use {@link #fromProjectQueries2} instead because
     *   {@code GradleProjectInfoQuery} is deprecated.
     * <P>
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
    @Deprecated
    public static GradleModelDef fromProjectQueries(org.netbeans.gradle.model.api.GradleProjectInfoQuery<?>... queries) {
        return new GradleModelDef(Collections.<Class<?>>emptyList(), Arrays.asList(queries));
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
    public static GradleModelDef fromProjectQueries2(GradleProjectInfoQuery2<?>... queries) {
        return create(Collections.<Class<?>>emptyList(), Arrays.asList(queries));
    }

    /**
     * Creates a new {@code GradleModelDef} with the given custom
     * {@link ProjectInfoBuilder} instances and
     * {@link #getToolingModels() Tooling API models}.
     * <P>
     * <B>Warning</B>: This method assumes that for each builder, the builder
     * and its result are found in the same classpath entry (usually jar) and
     * can be deserialized using the {@code ClassLoader} used to load the class
     * of the builder.
     *
     * @param modelTypes the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @param builders custom builders able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given custom builders.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public static GradleModelDef fromProjectInfoBuilders2(
            Collection<? extends Class<?>> modelTypes,
            ProjectInfoBuilder2<?>... builders) {

        List<GradleProjectInfoQuery2<?>> queries = new ArrayList<>(builders.length);
        for (ProjectInfoBuilder2<?> builder: builders) {
            queries.add(createDefaultQuery(builder));
        }

        return GradleModelDef.create(modelTypes, queries);
    }

    /**
     * Creates a new {@code GradleModelDef} with the given custom
     * {@link ProjectInfoBuilder} instances and with no
     * {@link #getToolingModels() Tooling API models}.
     * <P>
     * <B>Warning</B>: This method assumes that for each builder, the builder
     * and its result are found in the same classpath entry (usually jar) and
     * can be deserialized using the {@code ClassLoader} used to load the class
     * of the builder.
     *
     * @param builders custom builders able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given custom builders.
     *   This method never returns {@code null}.
     */
    @Nonnull
    public static GradleModelDef fromProjectInfoBuilders2(ProjectInfoBuilder2<?>... builders) {
        return fromProjectInfoBuilders2(Collections.<Class<?>>emptySet(), builders);
    }

    /**
     * @deprecated Use {@link #fromProjectInfoBuilders2} instead because
     *   {@code ProjectInfoBuilder} is deprecated.
     * <P>
     * Creates a new {@code GradleModelDef} with the given custom
     * {@link ProjectInfoBuilder} instances and
     * {@link #getToolingModels() Tooling API models}.
     * <P>
     * <B>Warning</B>: This method assumes that for each builder, the builder
     * and its result are found in the same classpath entry (usually jar) and
     * can be deserialized using the {@code ClassLoader} used to load the class
     * of the builder.
     *
     * @param modelTypes the Tooling API models to request from Gradle. For
     *   example: {@code IdeaProject}. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     * @param builders custom builders able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given custom builders.
     *   This method never returns {@code null}.
     */
    @Nonnull
    @Deprecated
    public static GradleModelDef fromProjectInfoBuilders(
            Collection<? extends Class<?>> modelTypes,
            org.netbeans.gradle.model.api.ProjectInfoBuilder<?>... builders) {

        return fromProjectInfoBuilders2(modelTypes, convertBuilders(builders));
    }

    /**
     * @deprecated Use {@link #fromProjectInfoBuilders2} instead because
     *   {@code ProjectInfoBuilder} is deprecated.
     * <P>
     * Creates a new {@code GradleModelDef} with the given custom
     * {@link ProjectInfoBuilder} instances and with no
     * {@link #getToolingModels() Tooling API models}.
     * <P>
     * <B>Warning</B>: This method assumes that for each builder, the builder
     * and its result are found in the same classpath entry (usually jar) and
     * can be deserialized using the {@code ClassLoader} used to load the class
     * of the builder.
     *
     * @param builders custom builders able to retrieve information
     *   from a {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the new {@code GradleModelDef} with the given custom builders.
     *   This method never returns {@code null}.
     */
    @Nonnull
    @Deprecated
    public static GradleModelDef fromProjectInfoBuilders(org.netbeans.gradle.model.api.ProjectInfoBuilder<?>... builders) {
        return fromProjectInfoBuilders2(Collections.<Class<?>>emptySet(), convertBuilders(builders));
    }

    @SuppressWarnings("deprecation")
    private static ProjectInfoBuilder2<?>[] convertBuilders(org.netbeans.gradle.model.api.ProjectInfoBuilder<?>... builders) {
        ProjectInfoBuilder2<?>[] converted = new ProjectInfoBuilder2<?>[builders.length];
        for (int i = 0; i < builders.length; i++) {
            converted[i] = CompatibilityUtils.toBuilder2(builders[i]);
        }
        return converted;
    }

    private static <T> GradleProjectInfoQuery2<T> createDefaultQuery(final ProjectInfoBuilder2<T> builder) {
        ExceptionHelper.checkNotNullArgument(builder, "builder");

        final ModelClassPathDef classPathDef = CompatibilityUtils.getClassPathOfBuilder(builder);

        return new GradleProjectInfoQuery2<T>() {
            @Override
            public ProjectInfoBuilder2<T> getInfoBuilder() {
                return builder;
            }

            @Override
            public ModelClassPathDef getInfoClassPath() {
                return classPathDef;
            }
        };
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
     * @deprecated Use {@link #getProjectInfoQueries2()} instead because
     *   {@code GradleProjectInfoQuery} is deprecated.
     * <P>
     * Returns the custom queries the retrieve information from Gradle projects
     * from a {@link org.gradle.api.Project} instance.
     *
     * @return the custom queries the retrieve information from Gradle projects.
     *   This method never returns {@code null} and the returned collection does
     *   not contain {@code null} elements.
     */
    @Nonnull
    @Deprecated
    public Collection<org.netbeans.gradle.model.api.GradleProjectInfoQuery<?>> getProjectInfoQueries() {
        return CompatibilityUtils.toQueryAll(projectInfoQueries);
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
    public Collection<GradleProjectInfoQuery2<?>> getProjectInfoQueries2() {
        return projectInfoQueries;
    }
}
