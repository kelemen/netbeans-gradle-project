package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.api.config.CustomProfileQuery;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.ui.CustomizerProvider;

public final class NbGradleSingleProjectConfigProvider
implements
        ProjectConfigurationProvider<NbGradleConfiguration>,
        ProjectInitListener {

    private final NbGradleProject project;
    private final NbGradleConfigProvider commonConfig;
    private volatile Set<NbGradleConfiguration> extensionProfiles;

    private NbGradleSingleProjectConfigProvider(
            NbGradleProject project,
            NbGradleConfigProvider multiProjectProvider) {
        if (project == null) throw new NullPointerException("project");
        if (multiProjectProvider == null) throw new NullPointerException("multiProjectProvider");

        this.project = project;
        this.commonConfig = multiProjectProvider;
        this.extensionProfiles = Collections.emptySet();
    }

    public static NbGradleSingleProjectConfigProvider create(NbGradleProject project) {
        return new NbGradleSingleProjectConfigProvider(
                project,
                NbGradleConfigProvider.getConfigProvider(project));
    }

    private void updateExtensionProfiles() {
        List<ProfileDef> customProfileDefs = new LinkedList<ProfileDef>();
        for (CustomProfileQuery profileQuery: project.getLookup().lookupAll(CustomProfileQuery.class)) {
            for (ProfileDef profileDef: profileQuery.getCustomProfiles()) {
                customProfileDefs.add(profileDef);
            }
        }

        Set<NbGradleConfiguration> customProfiles
                = new HashSet<NbGradleConfiguration>(2 * customProfileDefs.size());
        for (ProfileDef profileDef: customProfileDefs) {
            customProfiles.add(new NbGradleConfiguration(profileDef));
        }

        extensionProfiles = Collections.unmodifiableSet(customProfiles);
        commonConfig.fireConfigurationListChange();
    }

    @Override
    public void onInitProject() {
        project.addModelChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateExtensionProfiles();
            }
        });
    }

    @Override
    public Collection<NbGradleConfiguration> getConfigurations() {
        Collection<NbGradleConfiguration> commonProfiles = commonConfig.getConfigurations();
        Collection<NbGradleConfiguration> currentExtProfiles = extensionProfiles;

        List<NbGradleConfiguration> result
                = new ArrayList<NbGradleConfiguration>(commonProfiles.size() + currentExtProfiles.size());
        result.addAll(commonProfiles);
        result.addAll(currentExtProfiles);
        NbGradleConfiguration.sortProfiles(result);

        return result;
    }

    @Override
    public NbGradleConfiguration getActiveConfiguration() {
        NbGradleConfiguration config = commonConfig.getActiveConfiguration();
        if (!extensionProfiles.contains(config) && !commonConfig.getConfigurations().contains(config)) {
            return NbGradleConfiguration.DEFAULT_CONFIG;
        }
        return config;
    }

    @Override
    public void setActiveConfiguration(NbGradleConfiguration configuration) throws IOException {
        commonConfig.setActiveConfiguration(configuration);
    }

    private CustomizerProvider getCustomizerProvider() {
        return project.getLookup().lookup(CustomizerProvider.class);
    }

    @Override
    public boolean hasCustomizer() {
        return getCustomizerProvider() != null;
    }

    @Override
    public void customize() {
        CustomizerProvider customizerProvider = getCustomizerProvider();
        if (customizerProvider != null) {
            customizerProvider.showCustomizer();
        }
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
        List<NbGradleConfiguration> result = new LinkedList<NbGradleConfiguration>();
        result.addAll(extensionProfiles);
        result.addAll(commonConfig.findAndUpdateConfigurations(mayRemove));
        return result;
    }

    public void addActiveConfigChangeListener(ChangeListener listener) {
        commonConfig.addActiveConfigChangeListener(listener);
    }

    public void removeActiveConfigChangeListener(ChangeListener listener) {
        commonConfig.removeActiveConfigChangeListener(listener);
    }
}
