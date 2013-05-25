package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.ProjectConfigurationProvider;

public final class NbGradleSingleProjectConfigProvider
implements
        ProjectConfigurationProvider<NbGradleConfiguration> {

    private final NbGradleProject project;
    private final NbGradleConfigProvider commonConfig;

    private NbGradleSingleProjectConfigProvider(
            NbGradleProject project,
            NbGradleConfigProvider multiProjectProvider) {
        if (project == null) throw new NullPointerException("project");
        if (multiProjectProvider == null) throw new NullPointerException("multiProjectProvider");

        this.project = project;
        this.commonConfig = multiProjectProvider;
    }

    public static NbGradleSingleProjectConfigProvider create(NbGradleProject project) {
        return new NbGradleSingleProjectConfigProvider(
                project,
                NbGradleConfigProvider.getConfigProvider(project));
    }

    @Override
    public Collection<NbGradleConfiguration> getConfigurations() {
        return commonConfig.getConfigurations();
    }

    @Override
    public NbGradleConfiguration getActiveConfiguration() {
        return commonConfig.getActiveConfiguration();
    }

    @Override
    public void setActiveConfiguration(NbGradleConfiguration configuration) throws IOException {
        commonConfig.setActiveConfiguration(configuration);
    }

    @Override
    public boolean hasCustomizer() {
        return commonConfig.hasCustomizer();
    }

    @Override
    public void customize() {
        commonConfig.customize();
    }

    @Override
    public boolean configurationsAffectAction(String command) {
        return commonConfig.configurationsAffectAction(command);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener lst) {
        commonConfig.addPropertyChangeListener(lst);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener lst) {
        commonConfig.removePropertyChangeListener(lst);
    }

    public void removeConfiguration(NbGradleConfiguration config) {
        commonConfig.removeConfiguration(config);
    }

    public void addConfiguration(NbGradleConfiguration config) {
        commonConfig.addConfiguration(config);
    }

    public Collection<NbGradleConfiguration> findAndUpdateConfigurations(boolean mayRemove) {
        return commonConfig.findAndUpdateConfigurations(mayRemove);
    }

    public void addActiveConfigChangeListener(ChangeListener listener) {
        commonConfig.addActiveConfigChangeListener(listener);
    }

    public void removeActiveConfigChangeListener(ChangeListener listener) {
        commonConfig.removeActiveConfigChangeListener(listener);
    }
}
