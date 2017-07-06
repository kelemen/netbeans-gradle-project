package org.netbeans.gradle.project.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.modelquery.GradleModelDef;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery2;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;

public final class ModelNeeds {
    private static final Logger LOGGER = Logger.getLogger(ModelNeeds.class.getName());

    private final GradleModelDefQuery1 query1;
    private final GradleModelDefQuery2 query2;

    public ModelNeeds(GradleProjectExtensionDef<?> extensionDef) {
        Objects.requireNonNull(extensionDef, "extensionDef");
        this.query1 = createQuery1(extensionDef);
        this.query2 = createQuery2(extensionDef);
    }

    public GradleModelDefQuery1 getQuery1() {
        return query1;
    }

    public GradleModelDefQuery2 getQuery2() {
        return query2;
    }

    private static GradleModelDefQuery1 createQuery1(
            Collection<? extends GradleModelDefQuery1> extensionQueries) {

        final List<GradleModelDefQuery1> queries = CollectionUtils.copyNullSafeList(extensionQueries);

        int size = queries.size();

        if (size == 0) {
            return NoModels1.INSTANCE;
        }

        if (queries.size() == 1) {
            return queries.iterator().next();
        }

        return (GradleTarget gradleTarget) -> {
            List<Class<?>> result = new ArrayList<>();
            for (GradleModelDefQuery1 query: queries) {
                Collection<Class<?>> models = safelyReturn(query.getToolingModels(gradleTarget), query);
                result.addAll(models);
            }
            return result;
        };
    }

    private static GradleModelDefQuery1 createQuery1(final GradleProjectExtensionDef<?> extensionDef) {
        return createQuery1(extensionDef.getLookup().lookupAll(GradleModelDefQuery1.class));
    }

    private static GradleModelDefQuery2 query1AsQuery2(final GradleModelDefQuery1 query1) {
        return (GradleTarget gradleTarget) -> {
            Collection<Class<?>> toolinModels = query1.getToolingModels(gradleTarget);
            if (toolinModels.isEmpty()) {
                return GradleModelDef.EMPTY;
            }

            return GradleModelDef.create(
                    toolinModels,
                    Collections.<GradleProjectInfoQuery2<?>>emptyList());
        };
    }

    private static GradleModelDefQuery2 createQuery2(
            Collection<? extends GradleModelDefQuery2> extensionQueries) {
        final List<GradleModelDefQuery2> queries = CollectionUtils.copyNullSafeList(extensionQueries);

        int size = queries.size();

        if (size == 0) {
            return NoModels2.INSTANCE;
        }

        if (size == 1) {
            return queries.iterator().next();
        }

        return (GradleTarget gradleTarget) -> {
            List<GradleProjectInfoQuery2<?>> projectInfoQueries = new ArrayList<>();
            List<Class<?>> toolingModels = new ArrayList<>();

            for (GradleModelDefQuery2 query: queries) {
                GradleModelDef modelDef = safelyReturn(query.getModelDef(gradleTarget), query);

                projectInfoQueries.addAll(modelDef.getProjectInfoQueries2());
                toolingModels.addAll(modelDef.getToolingModels());
            }
            return GradleModelDef.create(toolingModels, projectInfoQueries);
        };
    }

    private static GradleModelDefQuery2 createQuery2(final GradleProjectExtensionDef<?> extension) {
        Collection<? extends GradleModelDefQuery2> queries2
                = extension.getLookup().lookupAll(GradleModelDefQuery2.class);
        if (!queries2.isEmpty()) {
            return createQuery2(queries2);
        }

        return query1AsQuery2(createQuery1(extension));
    }

    private static <T> Collection<T> safelyReturn(Collection<T> result, Object query) {
        if (result == null) {
            LOGGER.log(Level.WARNING,
                    "GradleModelDefQuery1.getToolingModels returned null for a query {0}",
                    query.getClass().getName());
            return Collections.emptyList();
        }
        else {
            return result;
        }
    }

    private static GradleModelDef safelyReturn(GradleModelDef result, Object query) {
        if (result == null) {
            LOGGER.log(Level.WARNING,
                    "GradleModelDefQuery2.getModelDef returned null for a query {0}",
                    query.getClass().getName());
            return GradleModelDef.EMPTY;
        }
        else {
            return result;
        }
    }

    private enum NoModels1 implements GradleModelDefQuery1 {
        INSTANCE;

        @Override
        public Collection<Class<?>> getToolingModels(GradleTarget gradleTarget) {
            return Collections.emptyList();
        }
    }

    private enum NoModels2 implements GradleModelDefQuery2 {
        INSTANCE;

        @Override
        public GradleModelDef getModelDef(GradleTarget gradleTarget) {
            return GradleModelDef.EMPTY;
        }
    }
}
