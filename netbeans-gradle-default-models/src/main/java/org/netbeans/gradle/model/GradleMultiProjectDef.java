package org.netbeans.gradle.model;

import java.io.Serializable;

public final class GradleMultiProjectDef implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GradleProjectTree rootProject;
    private final GradleProjectTree mainProject;

    public GradleMultiProjectDef(GradleProjectTree rootProject, GradleProjectTree mainProject) {
        if (rootProject == null) throw new NullPointerException("rootProject");
        if (mainProject == null) throw new NullPointerException("mainProject");

        this.rootProject = rootProject;
        this.mainProject = mainProject;
    }

    public GradleProjectTree getRootProject() {
        return rootProject;
    }

    public GradleProjectTree getMainProject() {
        return mainProject;
    }
}
