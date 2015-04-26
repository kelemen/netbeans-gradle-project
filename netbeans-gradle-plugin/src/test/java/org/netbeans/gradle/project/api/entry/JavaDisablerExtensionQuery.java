package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.ServiceProvider;

/**
 * @deprecated
 */
@ServiceProvider(service = GradleProjectExtensionQuery.class, position = 200)
@Deprecated
public final class JavaDisablerExtensionQuery implements GradleProjectExtensionQuery {
    @Override
    public GradleProjectExtension loadExtensionForProject(Project project) throws IOException {
        boolean disableJava = "empty-project".equals(project.getProjectDirectory().getNameExt());
        return new JavaDisablerExtension(disableJava);
    }
}
