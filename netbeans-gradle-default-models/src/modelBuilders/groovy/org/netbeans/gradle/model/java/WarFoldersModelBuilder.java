package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.api.plugins.WarPluginConvention;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

enum WarFoldersModelBuilder
implements
        ProjectInfoBuilder<WarFoldersModel> {

    INSTANCE;

    public WarFoldersModel getProjectInfo(Project project) {
        WarPluginConvention warPlugin = project.getConvention().findPlugin(WarPluginConvention.class);
        if (warPlugin == null) {
            return null;
        }
        return new WarFoldersModel(warPlugin.getWebAppDir());
    }

    /** {@inheritDoc } */
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }
}
