package org.netbeans.gradle.project.api.entry;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectServiceProvider;

@ProjectServiceProvider(
        projectType = GradleProjectIDs.MODULE_NAME,
        service = {MyCustomLookupEntry.class})
public final class MyCustomLookupEntry {
    private final Project project;

    public MyCustomLookupEntry(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
