package org.netbeans.gradle.model;

import java.io.File;

public final class GradleMultiProjectDef {
    private final GradleProjectTree rootProject;
    private final GradleProjectTree mainProject;

    public GradleMultiProjectDef(GradleProjectTree rootProject, GradleProjectTree mainProject) {
        if (rootProject == null) throw new NullPointerException("rootProject");
        if (mainProject == null) throw new NullPointerException("mainProject");

        this.rootProject = rootProject;
        this.mainProject = mainProject;
    }

    public static GradleMultiProjectDef createEmpty(File projectDir) {
        GradleProjectTree emptyTree = GradleProjectTree.createEmpty(projectDir);
        return new GradleMultiProjectDef(emptyTree, emptyTree);
    }


    public GradleProjectTree getRootProject() {
        return rootProject;
    }

    public GradleProjectTree getMainProject() {
        return mainProject;
    }

    public File getProjectDir() {
        return mainProject.getProjectDir();
    }
}
