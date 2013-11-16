package org.netbeans.gradle.project.model;

import java.util.Collection;
import java.util.Map;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;

// TODO: This interface must be redesigned to be allowed to be used by external plugins.
//
// Things to consider:
//
//   - The client might want to use different models for different
//     JVM and Gradle version.
//   - Multiple extensions might query the same model (really?).
//   - Client's model needs additional JAR files.
public interface CustomModelQuery {
    public Collection<Class<?>> getProjectModels();
    public Map<Object, GradleBuildInfoQuery<?>> getBuildInfoQueries();
    public Map<Object, GradleProjectInfoQuery<?>> getProjectInfoQueries();
}
