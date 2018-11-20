package org.netbeans.gradle.model.java;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.BuilderResult;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedModelsOrError;
import org.netbeans.gradle.model.FetchedProjectModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.CollectionUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.model.util.TestUtils.*;

public final class InfoQueries {
    private static final ClassLoader DEFAULT_CLASS_LOADER = InfoQueries.class.getClassLoader();

    private static ModelClassPathDef classPathFromClass(Class<?>... types) {
        return ModelClassPathDef.fromClasses(DEFAULT_CLASS_LOADER, Arrays.asList(types));
    }

    public static <T> GradleProjectInfoQuery2<T> toQueryWithKnownClassPath(final ProjectInfoBuilder2<T> builder) {
        return new GradleProjectInfoQuery2<T>() {
            @Override
            public ProjectInfoBuilder2<T> getInfoBuilder() {
                return builder;
            }

            @Override
            public ModelClassPathDef getInfoClassPath() {
                return ModelClassPathDef.EMPTY;
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toQueryWithKnownClassPath(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            @Override
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            @Override
            public ModelClassPathDef getInfoClassPath() {
                return ModelClassPathDef.EMPTY;
            }
        };
    }


    public static <T> GradleProjectInfoQuery2<T> toCustomQuery(final ProjectInfoBuilder2<T> builder) {
        return new GradleProjectInfoQuery2<T>() {
            @Override
            public ProjectInfoBuilder2<T> getInfoBuilder() {
                return builder;
            }

            @Override
            public ModelClassPathDef getInfoClassPath() {
                return classPathFromClass(builder.getClass());
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toCustomQuery(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            @Override
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            @Override
            public ModelClassPathDef getInfoClassPath() {
                return classPathFromClass(builder.getClass());
            }
        };
    }

    private static GenericModelFetcher buildInfoFetcher(BuildInfoBuilder<?>... builders) {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = CollectionUtils.newHashMap(builders.length);

        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = Collections.emptyMap();

        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            buildInfos.put(i, Collections.<GradleBuildInfoQuery<?>>singletonList(
                    InfoQueries.toCustomQuery(builders[i])));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher projectInfoFetcher(ProjectInfoBuilder2<?>... builders) {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = Collections.emptyMap();
        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos
                = CollectionUtils.newHashMap(builders.length);

        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            projectInfos.put(i, Collections.<GradleProjectInfoQuery2<?>>singletonList(
                    InfoQueries.toCustomQuery(builders[i])));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher basicInfoFetcher() {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos = Collections.emptyMap();
        Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = Collections.emptySet();

        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    private static <E> E getSingleElement(List<E> list) {
        return CollectionUtils.getSingleElement(list);
    }

    private static void verifyNoException(Throwable error) {
        if (error == null) {
            return;
        }

        AssertionError fail = new AssertionError("Expected no error");
        fail.initCause(error);
        throw fail;
    }

    public static void verifyNoError(FetchedProjectModels models) {
        verifyNoException(models.getIssue());
    }

    public static void verifyNoError(FetchedModels models) {
        verifyNoError(models.getDefaultProjectModels());
        for (FetchedProjectModels otherModels: models.getOtherProjectModels()) {
            verifyNoError(otherModels);
        }
    }

    public static FetchedModels verifyNoError(FetchedModelsOrError modelsOrError) {
        verifyNoException(modelsOrError.getBuildScriptEvaluationError());
        verifyNoException(modelsOrError.getUnexpectedError());

        verifyNoError(modelsOrError.getModels());

        FetchedModels result = modelsOrError.getModels();
        assertNotNull(result);
        return result;
    }

    public static BuilderResult fetchSingleBuildInfoWithError(
            ProjectConnection connection,
            BuildInfoBuilder<?> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = buildInfoFetcher(infoBuilder);
        FetchedModels models = verifyNoError(modelFetcher.getModels(connection, defaultInit()));

        assertTrue(models.getDefaultProjectModels().getProjectInfoResults().isEmpty());

        return getSingleElement(models.getBuildInfoResults().get(0));
    }

    public static BuilderResult fetchSingleProjectInfoWithError(
            ProjectConnection connection,
            ProjectInfoBuilder2<?> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = projectInfoFetcher(infoBuilder);
        FetchedModels models = verifyNoError(modelFetcher.getModels(connection, defaultInit()));

        assertTrue(models.getBuildInfoResults().isEmpty());

        return getSingleElement(models.getDefaultProjectModels().getProjectInfoResults().get(0));
    }

    public static <T> T fetchSingleProjectInfo(
            ProjectConnection connection,
            ProjectInfoBuilder2<T> infoBuilder) throws IOException {

        BuilderResult builderResult = fetchSingleProjectInfoWithError(connection, infoBuilder);

        @SuppressWarnings("unchecked")
        T result = builderResult != null
                ? (T)builderResult.getResultIfNoIssue()
                : null;
        return result;
    }

    public static <T> T fetchSingleBuildInfo(
            ProjectConnection connection,
            BuildInfoBuilder<T> infoBuilder) throws IOException {

        BuilderResult builderResult = fetchSingleBuildInfoWithError(connection, infoBuilder);

        @SuppressWarnings("unchecked")
        T result = builderResult != null
                ? (T)builderResult.getResultIfNoIssue()
                : null;
        return result;
    }

    public static GradleMultiProjectDef fetchProjectDef(
            ProjectConnection connection) throws IOException {

        GenericModelFetcher modelFetcher = basicInfoFetcher();
        FetchedModels models = verifyNoError(modelFetcher.getModels(connection, defaultInit()));

        return models.getDefaultProjectModels().getProjectDef();
    }

    private InfoQueries() {
        throw new AssertionError();
    }
}
