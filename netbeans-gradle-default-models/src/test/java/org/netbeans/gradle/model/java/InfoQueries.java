package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.BuilderResult;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.FetchedModelsOrError;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;

import static org.junit.Assert.*;
import static org.netbeans.gradle.model.util.TestUtils.defaultInit;

public final class InfoQueries {
    private static final ClassLoader DEFAULT_CLASS_LOADER = InfoQueries.class.getClassLoader();

    public static ModelClassPathDef classPathFromClass(Class<?> type) {
        File classpath = ClassLoaderUtils.findClassPathOfClass(type);
        if (ModelClassPathDef.isImplicitlyAssumed(classpath)) {
            return ModelClassPathDef.EMPTY;
        }

        return ModelClassPathDef.fromJarFiles(DEFAULT_CLASS_LOADER, Collections.singleton(classpath));
    }

    public static <T> GradleProjectInfoQuery<T> toQueryWithKnownClassPath(final ProjectInfoBuilder<T> builder) {
        return new GradleProjectInfoQuery<T>() {
            public ProjectInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public ModelClassPathDef getInfoClassPath() {
                return ModelClassPathDef.EMPTY;
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toQueryWithKnownClassPath(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public ModelClassPathDef getInfoClassPath() {
                return ModelClassPathDef.EMPTY;
            }
        };
    }


    public static <T> GradleProjectInfoQuery<T> toCustomQuery(final ProjectInfoBuilder<T> builder) {
        return new GradleProjectInfoQuery<T>() {
            public ProjectInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public ModelClassPathDef getInfoClassPath() {
                return classPathFromClass(builder.getClass());
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toCustomQuery(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public ModelClassPathDef getInfoClassPath() {
                return classPathFromClass(builder.getClass());
            }
        };
    }

    public static GenericModelFetcher buildInfoFetcher(BuildInfoBuilder<?>... builders) {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = CollectionUtils.newHashMap(builders.length);

        Map<Object, List<GradleProjectInfoQuery<?>>> projectInfos
                = Collections.emptyMap();

        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            buildInfos.put(i, Collections.<GradleBuildInfoQuery<?>>singletonList(
                    InfoQueries.toCustomQuery(builders[i])));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher projectInfoFetcher(ProjectInfoBuilder<?>... builders) {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos
                = Collections.emptyMap();
        Map<Object, List<GradleProjectInfoQuery<?>>> projectInfos
                = CollectionUtils.newHashMap(builders.length);

        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            projectInfos.put(i, Collections.<GradleProjectInfoQuery<?>>singletonList(
                    InfoQueries.toCustomQuery(builders[i])));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher basicInfoFetcher() {
        Map<Object, List<GradleBuildInfoQuery<?>>> buildInfos = Collections.emptyMap();
        Map<Object, List<GradleProjectInfoQuery<?>>> projectInfos = Collections.emptyMap();
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

    public static FetchedModels verifyNoError(FetchedModelsOrError modelsOrError) {
        verifyNoException(modelsOrError.getBuildScriptEvaluationError());
        verifyNoException(modelsOrError.getUnexpectedError());

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
            ProjectInfoBuilder<?> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = projectInfoFetcher(infoBuilder);
        FetchedModels models = verifyNoError(modelFetcher.getModels(connection, defaultInit()));

        assertTrue(models.getBuildInfoResults().isEmpty());

        return getSingleElement(models.getDefaultProjectModels().getProjectInfoResults().get(0));
    }

    public static <T> T fetchSingleProjectInfo(
            ProjectConnection connection,
            ProjectInfoBuilder<T> infoBuilder) throws IOException {

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
