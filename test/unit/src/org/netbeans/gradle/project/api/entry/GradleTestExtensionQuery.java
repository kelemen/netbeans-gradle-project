package org.netbeans.gradle.project.api.entry;

import org.netbeans.api.project.Project;

/**
 *
 * @author radim
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery.class)
public class GradleTestExtensionQuery implements GradleProjectExtensionQuery {

    @Override
    public GradleProjectExtension loadExtensionForProject(Project project) {
        return new GradleTestExtension();
    }
    
}
