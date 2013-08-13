package org.netbeans.gradle.project.properties;

/**
 *
 * @author Kelemen Attila
 */
public interface ProjectPropertySource {
    public ProjectProperties load(PropertiesLoadListener onLoadTask);
}
