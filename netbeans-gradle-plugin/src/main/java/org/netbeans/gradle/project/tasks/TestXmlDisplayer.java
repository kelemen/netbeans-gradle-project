package org.netbeans.gradle.project.tasks;

import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.others.GradleTestSession;

public final class TestXmlDisplayer {
    private final Project project;
    private final JavaExtension javaExt;

    public TestXmlDisplayer(Project project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.javaExt = JavaExtension.getJavaExtensionOfProject(project);
    }

    private String getProjectName() {
        return javaExt.getCurrentModel().getMainModule().getProperties().getProjectFullName();
    }

    public void displayReport() {
        GradleTestSession session = new GradleTestSession();
        session.newSession(getProjectName(), project);
        session.endSession();
    }
}
