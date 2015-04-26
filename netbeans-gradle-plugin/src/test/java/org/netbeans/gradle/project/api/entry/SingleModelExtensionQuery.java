package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.netbeans.api.project.Project;

/**
 * @deprecated
 */
@Deprecated
public final class SingleModelExtensionQuery implements GradleProjectExtensionQuery {
    @Override
    public GradleProjectExtension loadExtensionForProject(Project project) throws IOException {
        return new SingleModelExtension(EclipseProject.class);
    }
}
