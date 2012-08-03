package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.openide.util.ImageUtilities;

public final class GradleProjectInformation implements ProjectInformation {
    @StaticResource
    private static final String PROJECT_ICON_PATH = "org/netbeans/gradle/project/resources/gradle.png";

    private final NbGradleProject project;

    public GradleProjectInformation(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(ImageUtilities.loadImage(PROJECT_ICON_PATH));
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
