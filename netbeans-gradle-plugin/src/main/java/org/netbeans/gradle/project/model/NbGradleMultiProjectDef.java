package org.netbeans.gradle.project.model;

import java.io.File;

public final class NbGradleMultiProjectDef {
    private final NbGradleProjectTree rootProject;
    private final NbGradleProjectTree mainProject;

    public NbGradleMultiProjectDef(NbGradleProjectTree rootProject, NbGradleProjectTree mainProject) {
        if (rootProject == null) throw new NullPointerException("rootProject");
        if (mainProject == null) throw new NullPointerException("mainProject");

        this.rootProject = rootProject;
        this.mainProject = mainProject;
    }

    public static NbGradleMultiProjectDef createEmpty(File projectDir) {
        NbGradleProjectTree emptyTree = NbGradleProjectTree.createEmpty(projectDir);
        return new NbGradleMultiProjectDef(emptyTree, emptyTree);
    }


    public NbGradleProjectTree getRootProject() {
        return rootProject;
    }

    public NbGradleProjectTree getMainProject() {
        return mainProject;
    }

    public File getProjectDir() {
        return mainProject.getProjectDir();
    }
}
