package org.netbeans.gradle.project.model;

public interface ProjectModelUpdatedListener {
    public void onUpdateProject(NbGradleModel newModel);
}
