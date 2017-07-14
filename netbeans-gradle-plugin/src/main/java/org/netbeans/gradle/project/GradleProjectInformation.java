package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.util.function.Supplier;
import javax.swing.Icon;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.utils.LazyValues;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.gradle.project.properties.SwingPropertyChangeForwarder;

public final class GradleProjectInformation implements ProjectInformation {
    private final NbGradleProject project;
    private final Supplier<SwingPropertyChangeForwarder> changeListenersRef;

    public GradleProjectInformation(NbGradleProject project) {
        this.project = project;

        this.changeListenersRef = LazyValues.lazyValue(() -> {
            SwingPropertyChangeForwarder.Builder combinedListeners
                    = new SwingPropertyChangeForwarder.Builder(SwingExecutors.getStrictExecutor(false));
            PropertySource<String> displayName = PropertyFactory.lazilyNotifiedSource(project.getDisplayInfo().displayName());
            combinedListeners.addProperty(PROP_DISPLAY_NAME, displayName);
            return combinedListeners.create();
        });
    }

    private SwingPropertyChangeForwarder getChangeListeners() {
        return changeListenersRef.get();
    }

    @Override
    public Icon getIcon() {
        return NbIcons.getGradleIconAsIcon();
    }

    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public String getDisplayName() {
        return project.getDisplayInfo().displayName().getValue();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        getChangeListeners().addPropertyChangeListener(pcl);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        getChangeListeners().removePropertyChangeListener(pcl);
    }

    @Override
    public Project getProject() {
        return project;
    }

}
