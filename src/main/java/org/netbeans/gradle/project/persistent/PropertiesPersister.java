package org.netbeans.gradle.project.persistent;

import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.openide.util.RequestProcessor;

public interface PropertiesPersister {
    // These methods may only be called from this processor
    public static final RequestProcessor PERSISTER_PROCESSOR
            = new RequestProcessor("Gradle-properties-persister", 1, true);

    public void save(NbGradleProject project, ProjectProperties properties, Runnable onDone);
    public void load(ProjectProperties properties, boolean usedConcurrently, Runnable onDone);
}
