package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;

public final class GradleProjectInformation implements ProjectInformation {
    private static final Icon GRADLE_ICON = new ImageIcon(NbIcons.getGradleIcon());

    private final NbGradleProject project;

    public GradleProjectInformation(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public Icon getIcon() {
        return GRADLE_ICON;
    }

    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public String getDisplayName() {
        return project.getDisplayName();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        //do nothing, won't change
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        //do nothing, won't change
    }

    @Override
    public Project getProject() {
        return project;
    }

}
