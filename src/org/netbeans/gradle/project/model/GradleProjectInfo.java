package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.gradle.tooling.model.GradleProject;
import org.netbeans.gradle.project.CollectionUtils;

public final class GradleProjectInfo {
    private final GradleProject gradleProject;
    private final File projectDir;
    private final List<GradleProjectInfo> children;

    public GradleProjectInfo(
            GradleProject gradleProject,
            File projectDir,
            Collection<GradleProjectInfo> children) {
        if (gradleProject == null) throw new NullPointerException("gradleProject");
        if (projectDir == null) throw new NullPointerException("projectDir");

        this.gradleProject = gradleProject;
        this.projectDir = projectDir;
        this.children = CollectionUtils.copyNullSafeList(children);
    }

    public static GradleProjectInfo createEmpty(File projectDir) {
        return new GradleProjectInfo(
                new EmptyGradleProject(projectDir),
                projectDir,
                Collections.<GradleProjectInfo>emptyList());
    }

    public GradleProject getGradleProject() {
        return gradleProject;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public List<GradleProjectInfo> getChildren() {
        return children;
    }
}
