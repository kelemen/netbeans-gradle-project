package org.netbeans.gradle.project.query;

import org.netbeans.gradle.project.api.entry.ProjectPlatform;

public interface J2SEPlatformFromScriptQuery {
    public ProjectPlatform getPlatform();
    public String getSourceLevel();
}
