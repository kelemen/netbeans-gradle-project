package org.netbeans.gradle.model.internal;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.BuilderResult;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BasicFileUtils;
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

    private static String getSafeToString(Object obj) {
        String className = obj.getClass().getSimpleName();
        String toStringValue = obj.toString();

        if (toStringValue == null) {
            return className;
        }
        else if (toStringValue.contains(className)) {
            return toStringValue;
        }
        else {
            return className + ": " + toStringValue;
        }
    }

    private static String getNameOfBuilder(ProjectInfoBuilder<?> builder) {
        if (builder == null) {
            return "null";
        }

        String name = builder.getName();
        // Although getName should never return null,
        // do something sensible anyway just to be safe.
        return name != null
                ? name
                : getSafeToString(builder);
    }


    private static BuilderIssue createIssue(
            ProjectInfoBuilder<?> builder,
            Throwable issue) {
        if (issue == null) {
            return null;
        }

        return new BuilderIssue(getNameOfBuilder(builder), issue);
    }

    private CustomSerializedMap fetchProjectInfos(Project project) {
        Map<Object, List<?>> projectInfoRequests = input.getProjectInfoRequests(classLoader);
        int requestCount = projectInfoRequests.size();
        CustomSerializedMap.Builder projectInfosBuilder = new CustomSerializedMap.Builder(requestCount);

        for (Map.Entry<?, List<?>> entry: projectInfoRequests.entrySet()) {
            Object key = entry.getKey();
            for (Object projectInfoBuilder: entry.getValue()) {
                Object info = null;
                Throwable issue = null;
                ProjectInfoBuilder<?> builder = null;

                try {
                    builder = (ProjectInfoBuilder<?>)projectInfoBuilder;
                    info = builder.getProjectInfo(project);
                } catch (Throwable ex) {
                    issue = ex;
                }

                if (info != null || issue != null) {
                    projectInfosBuilder.addValue(key, new BuilderResult(info, createIssue(builder, issue)));
                }
            }
        }

        return projectInfosBuilder.create();
    }

    public Object buildAll(String modelName, Project project) {
        if (!canBuild(modelName)) {
            throw new IllegalArgumentException("Unsupported model: " + modelName);
        }

        File buildScript = BasicFileUtils.toCanonicalFile(project.getBuildFile());

        ModelQueryOutput output;
        try {
            CustomSerializedMap projectInfos = fetchProjectInfos(project);
            output = new ModelQueryOutput(project.getPath(), buildScript, projectInfos, null);
        } catch (Throwable ex) {
            output = new ModelQueryOutput(project.getPath(), buildScript, CustomSerializedMap.EMPTY, ex);
        }

        return new DefaultModelQueryOutputRef(output);
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
