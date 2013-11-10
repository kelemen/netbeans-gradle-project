package org.netbeans.gradle.model.java;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.GradleProjectInfoQuery;
import org.netbeans.gradle.model.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.ClassLoaderUtils;

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

    private InfoQueries() {
        throw new AssertionError();
    }
}
