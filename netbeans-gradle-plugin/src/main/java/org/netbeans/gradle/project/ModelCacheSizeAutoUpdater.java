package org.netbeans.gradle.project;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.model.DefaultGradleModelLoader;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleMultiProjectDef;
import org.netbeans.gradle.project.model.ProjectModelChangeListener;
import org.netbeans.gradle.project.util.NbTaskExecutors;

public final class ModelCacheSizeAutoUpdater implements ProjectModelChangeListener {
    private static final ModelCacheSizeAutoUpdater DEFAULT = new ModelCacheSizeAutoUpdater(
            NbTaskExecutors.newDefaultUpdateExecutor(),
            DefaultGradleModelLoader::ensureCacheSize);

    private final UpdateTaskExecutor cacheSizeCheckExecutor;
    private final Consumer<? super Integer> cacheSizeUpdater;

    public ModelCacheSizeAutoUpdater(TaskExecutor cacheSizeCheckExecutor, Consumer<? super Integer> cacheSizeUpdater) {
        this(new GenericUpdateTaskExecutor(cacheSizeCheckExecutor), cacheSizeUpdater);
    }

    public ModelCacheSizeAutoUpdater(UpdateTaskExecutor cacheSizeCheckExecutor, Consumer<? super Integer> cacheSizeUpdater) {
        this.cacheSizeCheckExecutor = Objects.requireNonNull(cacheSizeCheckExecutor, "cacheSizeCheckExecutor");
        this.cacheSizeUpdater = Objects.requireNonNull(cacheSizeUpdater, "cacheSizeUpdater");
    }

    public static ModelCacheSizeAutoUpdater getDefault() {
        return DEFAULT;
    }

    @Override
    public void onModelChanged() {
        cacheSizeCheckExecutor.execute(this::checkCacheSize);
    }

    private void checkCacheSize() {
        Project[] projects = OpenProjects.getDefault().getOpenProjects();
        Map<Path, Integer> projectSizes = getProjectSizes(projects);

        int requiredCacheSize = sum(projectSizes.values());
        cacheSizeUpdater.accept(requiredCacheSize);
    }

    private static int sum(Collection<Integer> values) {
        int result = 0;
        for (Integer value: values) {
            result += value;
        }
        return result;
    }

    private static Map<Path, Integer> getProjectSizes(Project[] projects) {
        Map<Path, Integer> projectSizes = null;
        for (Project project: projects) {
            NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
            if (gradleProject != null && gradleProject.wasModelEverSet()) {
                NbGradleModel projectModel = gradleProject.currentModel().getValue();
                NbGradleMultiProjectDef projectDef = projectModel.getGenericInfo().getProjectDef();
                int numberOfProjectsInThisBuild = projectDef.getNumberOfProjectsInThisBuild();

                if (projectSizes == null) {
                    projectSizes = new HashMap<>();
                }
                // +1 for a possible buildSrc
                projectSizes.put(projectModel.getSettingsDir(), numberOfProjectsInThisBuild + 1);
            }
        }

        return projectSizes != null ? projectSizes : Collections.<Path, Integer>emptyMap();
    }
}
