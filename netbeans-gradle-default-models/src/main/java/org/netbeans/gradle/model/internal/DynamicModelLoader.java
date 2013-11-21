package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.SerializationUtils;

public final class DynamicModelLoader implements ToolingModelBuilder {
    private final ModelQueryInput input;
    private final ClassLoader classLoader;

    public DynamicModelLoader(ModelQueryInput input, ClassLoader classLoader) {
        if (input == null) throw new NullPointerException("input");

        this.input = input;
        this.classLoader = classLoader;
    }

    public boolean canBuild(String modelName) {
        return modelName.equals(ModelQueryOutputRef.class.getName());
    }

    public Object buildAll(String modelName, Project project) {
        if (!canBuild(modelName)) {
            throw new IllegalArgumentException("Unsupported model: " + modelName);
        }

        Map<Object, List<?>> projectInfoRequests = input.getProjectInfoRequests(classLoader);
        int requestCount = projectInfoRequests.size();
        CustomSerializedMap.Builder projectInfoResults = new CustomSerializedMap.Builder(requestCount);

        for (Map.Entry<?, List<?>> entry: projectInfoRequests.entrySet()) {
            // TODO: Catch exceptions for each entry and somehow report it back.

            Object key = entry.getKey();
            for (Object projectInfoBuilder: entry.getValue()) {
                Object info = ((ProjectInfoBuilder<?>)projectInfoBuilder).getProjectInfo(project);
                if (info != null) {
                    projectInfoResults.addValue(key, info);
                }
            }
        }

        return new DefaultModelQueryOutputRef(new ModelQueryOutput(project.getPath(), projectInfoResults.create()));
    }

    private static final class DefaultModelQueryOutputRef implements ModelQueryOutputRef, Serializable {
        private static final long serialVersionUID = 1L;

        private final ModelQueryOutput modelQueryOutput;

        public DefaultModelQueryOutputRef(ModelQueryOutput modelQueryOutput) {
            this.modelQueryOutput = modelQueryOutput;
        }

        public byte[] getSerializedModelQueryOutput() {
            return SerializationUtils.serializeObject(modelQueryOutput);
        }
    }
}
