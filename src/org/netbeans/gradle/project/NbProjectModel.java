package org.netbeans.gradle.project;

import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

public interface NbProjectModel {
    public EclipseProject getEclipseModel();
    public IdeaProject getIdeaModel();
}
