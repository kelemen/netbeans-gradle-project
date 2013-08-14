package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

public final class NbJavaGradleModelBuilder implements ToolingModelBuilder {
    public boolean canBuild(String modelName) {
        return NbJavaGradleModel.class.getName().equals(modelName);
    }

    public Object buildAll(String modelName, Project project) {
        if (!canBuild(modelName)) {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }

        return new NbJavaGradleModelImpl();
    }
}
