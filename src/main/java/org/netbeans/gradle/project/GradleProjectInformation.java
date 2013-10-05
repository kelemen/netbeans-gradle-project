package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.openide.util.ImageUtilities;

public final class GradleProjectInformation implements ProjectInformation {

    private final NbGradleProject project;

    public GradleProjectInformation(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public Icon getIcon() {
        return ImageUtilities.image2Icon(NbIcons.getGradleIcon());
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
