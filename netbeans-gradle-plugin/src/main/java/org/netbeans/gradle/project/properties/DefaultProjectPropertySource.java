package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.NbGradleProject;

public final class DefaultProjectPropertySource implements ProjectPropertySource {
    private final ProjectProperties properties;

    public DefaultProjectPropertySource(NbGradleProject project) {
        this.properties = new DefaultProjectProperties(project);
    }

    @Override
    public ProjectProperties load(PropertiesLoadListener onLoadTask) {
        if (onLoadTask != null) {
            onLoadTask.loadedProperties(properties);
        }
        return properties;
    }
}
