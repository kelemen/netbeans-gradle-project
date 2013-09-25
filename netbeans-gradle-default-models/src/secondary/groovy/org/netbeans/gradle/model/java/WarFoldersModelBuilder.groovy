package org.netbeans.gradle.model.java

import org.gradle.api.*;
import org.netbeans.gradle.model.ProjectInfoBuilder;

public enum WarFoldersModelBuilder
implements
        ProjectInfoBuilder<WarFoldersModel> {

    INSTANCE;

    private static Class<?> getApiClass(Project project, String className) {
        return project.getClass().classLoader.loadClass(className);
    }

    public WarFoldersModel getProjectInfo(Project project) {
        if (!project.plugins.hasPlugin('war')) {
            return null;
        }

        return new WarFoldersModel(project.webAppDir);
    }
}
