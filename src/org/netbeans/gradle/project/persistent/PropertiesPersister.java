package org.netbeans.gradle.project.persistent;

import org.netbeans.gradle.project.properties.ProjectProperties;

public interface PropertiesPersister {
    // These methods may only be called from the EDT

    public void save(ProjectProperties properties, Runnable onDone);
    public void load(ProjectProperties properties, Runnable onDone);
}
