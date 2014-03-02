package org.netbeans.gradle.project.persistent;

import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.properties.ProjectProperties;

public interface PropertiesPersister {
    // These methods may only be called from this processor
    public static final MonitorableTaskExecutorService PERSISTER_PROCESSOR
            = NbTaskExecutors.newExecutor("Gradle-properties-persister", 1);

    public void save(NbGradleProject project, ProjectProperties properties, Runnable onDone);
    public void load(ProjectProperties properties, boolean usedConcurrently, Runnable onDone);
}
