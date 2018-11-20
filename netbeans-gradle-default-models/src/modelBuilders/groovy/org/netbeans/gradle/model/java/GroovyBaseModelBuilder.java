package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.api.plugins.GroovyPlugin;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;

enum GroovyBaseModelBuilder implements ProjectInfoBuilder2<GroovyBaseModel> {
    INSTANCE;

    @Override
    public GroovyBaseModel getProjectInfo(Object project) {
        return getProjectInfo((Project)project);
    }

    private GroovyBaseModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin(GroovyPlugin.class)) {
            return null;
        }

        return GroovyBaseModel.DEFAULT;
    }

    @Override
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }
}
