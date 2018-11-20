package org.netbeans.gradle.model.internal;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.netbeans.gradle.model.BuilderResult;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.model.util.BuilderUtils;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationCaches;
import org.netbeans.gradle.model.util.SerializationUtils;

public final class DynamicModelLoader implements ToolingModelBuilder {
    private final ModelQueryInput input;
    private final ClassLoader classLoader;

    public DynamicModelLoader(ModelQueryInput input, ClassLoader classLoader) {
        if (input == null) throw new NullPointerException("input");

        this.input = input;
        this.classLoader = classLoader;
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(ModelQueryOutputRef.class.getName());
    }

    private CustomSerializedMap fetchProjectInfos(Project project) {
        SerializationCache serializationCache = SerializationCaches.getDefault();
        Map<Object, List<?>> projectInfoRequests = input.getProjectInfoRequests(serializationCache, classLoader);
        int requestCount = projectInfoRequests.size();
        CustomSerializedMap.Builder projectInfosBuilder = new CustomSerializedMap.Builder(requestCount);

        for (Map.Entry<?, List<?>> entry: projectInfoRequests.entrySet()) {
            Object key = entry.getKey();
            for (Object projectInfoBuilder: entry.getValue()) {
                Object info = null;
                Throwable issue = null;
                ProjectInfoBuilder2<?> builder = null;

                try {
                    builder = (ProjectInfoBuilder2<?>)projectInfoBuilder;
                    info = builder.getProjectInfo(project);
                } catch (Throwable ex) {
                    issue = ex;
                }

                if (info != null || issue != null) {
                    BuilderResult builderResult = new BuilderResult(
                            info,
                            BuilderUtils.createIssue(builder, issue));
                    projectInfosBuilder.addValue(key, builderResult);
                }
            }
        }

        return projectInfosBuilder.create();
    }

    private Collection<GradleTaskID> findTasks(Project project) {
        TaskContainer tasks = project.getTasks();

        // Note: This might cause failures in Gradle 2.4-rc-1
        // due to GRADLE-3293. (in practice however, we do not request
        // custom models and GradleProject together).
        List<GradleTaskID> result = new ArrayList<GradleTaskID>(tasks.size());
        for (String taskName: tasks.getNames()) {
            Task task = tasks.findByName(taskName);
            if (task != null) {
                String name = task.getName();
                String fullName = task.getPath();
                result.add(new GradleTaskID(name, fullName));
            }
        }

        return result;
    }

    private static String toSafeString(Object obj) {
        String result = obj != null ? obj.toString() : null;
        return result != null ? result : "";
    }

    private static ProjectId getProjectId(Project project) {
        String group = toSafeString(project.getGroup());
        String name = toSafeString(project.getName());
        String version = toSafeString(project.getVersion());
        return new ProjectId(group, name, version);
    }

    private BasicInfoWithError getBasicInfo(Project project) {
        File buildDir = project.getBuildDir();
        String projectFullName = project.getPath();
        ProjectId projectId = getProjectId(project);

        File buildFile = null;
        Collection<GradleTaskID> tasks = Collections.emptyList();

        Throwable error = null;
        try {
            buildFile = BasicFileUtils.toCanonicalFile(project.getBuildFile());
            tasks = findTasks(project);
        } catch (Throwable ex) {
            error = ex;
        }

        ModelQueryOutput.BasicInfo result = new ModelQueryOutput.BasicInfo(projectId, projectFullName, buildFile, buildDir, tasks);
        return new BasicInfoWithError(result, error);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        if (!canBuild(modelName)) {
            throw new IllegalArgumentException("Unsupported model: " + modelName);
        }

        BasicInfoWithError basicInfo = getBasicInfo(project);

        ModelQueryOutput output;
        try {
            CustomSerializedMap projectInfos = fetchProjectInfos(project);
            output = new ModelQueryOutput(basicInfo.info, projectInfos, basicInfo.error);
        } catch (Throwable ex) {
            if (basicInfo.error != null) {
                Exceptions.tryAddSuppressedException(ex, basicInfo.error);
            }
            output = new ModelQueryOutput(basicInfo.info, CustomSerializedMap.EMPTY, ex);
        }

        return new DefaultModelQueryOutputRef(output);
    }

    private static final class BasicInfoWithError {
        public final ModelQueryOutput.BasicInfo info;
        public final Throwable error;

        public BasicInfoWithError(ModelQueryOutput.BasicInfo info, Throwable error) {
            this.info = info;
            this.error = error;
        }
    }

    private static final class DefaultModelQueryOutputRef implements ModelQueryOutputRef, Serializable {
        private static final long serialVersionUID = 1L;

        private final ModelQueryOutput modelQueryOutput;

        public DefaultModelQueryOutputRef(ModelQueryOutput modelQueryOutput) {
            this.modelQueryOutput = modelQueryOutput;
        }

        @Override
        public byte[] getSerializedModelQueryOutput() {
            return SerializationUtils.serializeObject(modelQueryOutput);
        }
    }
}
