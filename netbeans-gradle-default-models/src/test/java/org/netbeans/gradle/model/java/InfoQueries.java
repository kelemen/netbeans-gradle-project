package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.ProjectConnection;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.FetchedModels;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.GradleProjectInfoQuery;
import org.netbeans.gradle.model.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.ClassLoaderUtils;

import static org.junit.Assert.assertTrue;
import static org.netbeans.gradle.model.util.TestUtils.defaultInit;

public final class InfoQueries {
    public static <T> GradleProjectInfoQuery<T> toBuiltInQuery(final ProjectInfoBuilder<T> builder) {
        return new GradleProjectInfoQuery<T>() {
            public ProjectInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.emptySet();
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toBuiltInQuery(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.singleton(ClassLoaderUtils.findClassPathOfClass(builder.getClass()));
            }
        };
    }

    public static <T> GradleProjectInfoQuery<T> toCustomQuery(final ProjectInfoBuilder<T> builder) {
        return new GradleProjectInfoQuery<T>() {
            public ProjectInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.singleton(ClassLoaderUtils.findClassPathOfClass(builder.getClass()));
            }
        };
    }

    public static <T> GradleBuildInfoQuery<T> toCustomQuery(final BuildInfoBuilder<T> builder) {
        return new GradleBuildInfoQuery<T>() {
            public BuildInfoBuilder<T> getInfoBuilder() {
                return builder;
            }

            public Set<File> getInfoClassPath() {
                return Collections.singleton(ClassLoaderUtils.findClassPathOfClass(builder.getClass()));
            }
        };
    }

    public static GenericModelFetcher buildInfoFetcher(BuildInfoBuilder<?>... builders) {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = new HashMap<Object, GradleBuildInfoQuery<?>>();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            buildInfos.put(i, InfoQueries.toBuiltInQuery(builders[i]));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher projectInfoFetcher(ProjectInfoBuilder<?>... builders) {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = new HashMap<Object, GradleProjectInfoQuery<?>>();
        Set<Class<?>> toolingModels = Collections.emptySet();

        for (int i = 0; i < builders.length; i++) {
            projectInfos.put(i, InfoQueries.toBuiltInQuery(builders[i]));
        }
        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static GenericModelFetcher basicInfoFetcher() {
        Map<Object, GradleBuildInfoQuery<?>> buildInfos = Collections.emptyMap();
        Map<Object, GradleProjectInfoQuery<?>> projectInfos = Collections.emptyMap();
        Set<Class<?>> toolingModels = Collections.emptySet();

        return new GenericModelFetcher(buildInfos, projectInfos, toolingModels);
    }

    public static <T> T fetchSingleProjectInfo(
            ProjectConnection connection,
            ProjectInfoBuilder<T> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = projectInfoFetcher(infoBuilder);
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        assertTrue(models.getBuildInfoResults().isEmpty());

        @SuppressWarnings("unchecked")
        T result = (T)models.getDefaultProjectModels().getProjectInfoResults().get(0);
        return result;
    }

    public static <T> T fetchSingleBuildInfo(
            ProjectConnection connection,
            BuildInfoBuilder<T> infoBuilder) throws IOException {

        GenericModelFetcher modelFetcher = buildInfoFetcher(infoBuilder);
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        @SuppressWarnings("unchecked")
        T result = (T)models.getBuildInfoResults().get(0);
        return result;
    }

    public static GradleMultiProjectDef fetchProjectDef(
            ProjectConnection connection) throws IOException {

        GenericModelFetcher modelFetcher = basicInfoFetcher();
        FetchedModels models = modelFetcher.getModels(connection, defaultInit());

        return models.getDefaultProjectModels().getProjectDef();
    }

    private InfoQueries() {
        throw new AssertionError();
    }
}
