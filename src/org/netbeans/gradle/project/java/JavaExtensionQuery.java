package org.netbeans.gradle.project.java;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectExtensionQuery.class, position = 1000)
public final class JavaExtensionQuery implements GradleProjectExtensionQuery {
    public JavaExtensionQuery() {
    }

    @Override
    public GradleProjectExtension loadExtensionForProject(Project project) throws IOException {
        return JavaExtension.create(project);
    }
}
