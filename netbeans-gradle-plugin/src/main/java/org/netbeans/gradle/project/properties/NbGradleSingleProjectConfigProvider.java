package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectModelChangeListener;
import org.netbeans.gradle.project.api.config.CustomProfileQuery;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.ui.CustomizerProvider;

public final class NbGradleSingleProjectConfigProvider
implements
        ProjectConfigurationProvider<NbGradleConfiguration>,
        ProjectModelChangeListener {

    private final NbGradleProject project;
    private final AtomicReference<Path> currentRootDir;
    private final MutableProperty<NbGradleConfigProvider> commonConfigRef;

    private final PropertySource<NbGradleConfiguration> activeConfig;
    private final PropertySource<Collection<NbGradleConfiguration>> configs;
    private final ActiveSettingsQueryEx activeSettingsQuery;

    private final SwingPropertyChangeForwarder propertyChangeForwarder;
    private volatile Set<NbGradleConfiguration> extensionProfiles;

    private NbGradleSingleProjectConfigProvider(
            NbGradleProject project,
            NbGradleConfigProvider multiProjectProvider) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(multiProjectProvider, "multiProjectProvider");

        this.project = project;
        this.currentRootDir = new AtomicReference<>(multiProjectProvider.getRootDirectory());
        this.commonConfigRef = PropertyFactory.memPropertyConcurrent(multiProjectProvider, SwingTaskExecutor.getStrictExecutor(false));
        this.extensionProfiles = Collections.emptySet();

        this.activeConfig = NbProperties.propertyOfProperty(commonConfigRef, new NbFunction<NbGradleConfigProvider, PropertySource<NbGradleConfiguration>>() {
            @Override
            public PropertySource<NbGradleConfiguration> apply(NbGradleConfigProvider arg) {
                return arg.activeConfiguration();
            }
        });

        this.configs = NbProperties.propertyOfProperty(commonConfigRef, new NbFunction<NbGradleConfigProvider, PropertySource<Collection<NbGradleConfiguration>>>() {
            @Override
            public PropertySource<Collection<NbGradleConfiguration>> apply(NbGradleConfigProvider arg) {
                return arg.configurations();
            }
        });

        PropertySource<ActiveSettingsQueryEx> activeSettingsQueryRef;
        activeSettingsQueryRef = PropertyFactory.convert(commonConfigRef, new ValueConverter<NbGradleConfigProvider, ActiveSettingsQueryEx>() {
            @Override
            public ActiveSettingsQueryEx convert(NbGradleConfigProvider arg) {
                return arg.getActiveSettingsQuery();
            }
        });
        this.activeSettingsQuery = new ActiveSettingsQueryExProxy(activeSettingsQueryRef);

        this.propertyChangeForwarder = createPropertyChanges();
    }

    private SwingPropertyChangeForwarder createPropertyChanges() {
        SwingPropertyChangeForwarder.Builder result = new SwingPropertyChangeForwarder.Builder();

        result.addProperty(PROP_CONFIGURATION_ACTIVE, activeConfig, this);
        result.addPropertyNoValue(PROP_CONFIGURATIONS, configs, this);

        return result.create();
    }

    public static NbGradleSingleProjectConfigProvider create(NbGradleProject project) {
        Path rootDir = SettingsFiles.getRootDirectory(project);

        return new NbGradleSingleProjectConfigProvider(
                project,
                NbGradleConfigProvider.getConfigProvider(rootDir));
    }

    private NbGradleConfigProvider getCommonConfig() {
        return commonConfigRef.getValue();
    }

    public ActiveSettingsQueryEx getActiveSettingsQuery() {
        return activeSettingsQuery;
    }

    public ProfileSettingsContainer getProfileSettingsContainer() {
        return getCommonConfig().getProfileSettingsContainer();
    }

    private void updateExtensionProfiles() {
        List<ProfileDef> customProfileDefs = new LinkedList<>();
        for (CustomProfileQuery profileQuery: project.getLookup().lookupAll(CustomProfileQuery.class)) {
            for (ProfileDef profileDef: profileQuery.getCustomProfiles()) {
                customProfileDefs.add(profileDef);
            }
        }

        Set<NbGradleConfiguration> customProfiles = CollectionUtils.newHashSet(customProfileDefs.size());
        for (ProfileDef profileDef: customProfileDefs) {
            customProfiles.add(new NbGradleConfiguration(profileDef));
        }

        extensionProfiles = Collections.unmodifiableSet(customProfiles);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                propertyChangeForwarder.firePropertyChange(new PropertyChangeEvent(this, PROP_CONFIGURATIONS, null, null));
            }
        });
    }

    @Override
    public void onModelChanged() {
        NbGradleModel currentModel = project.currentModel().getValue();
        Path newRootDir = currentModel.getRootProjectDir().toPath();
        Path prevRootDir = currentRootDir.getAndSet(newRootDir);

        if (!Objects.equals(prevRootDir, newRootDir)) {
            commonConfigRef.setValue(NbGradleConfigProvider.getConfigProvider(newRootDir));
        }

        updateExtensionProfiles();
    }

    @Override
    public Collection<NbGradleConfiguration> getConfigurations() {
        Collection<NbGradleConfiguration> commonProfiles = getCommonConfig().getConfigurations();
        Collection<NbGradleConfiguration> currentExtProfiles = extensionProfiles;

        List<NbGradleConfiguration> result
                = new ArrayList<>(commonProfiles.size() + currentExtProfiles.size());
        result.addAll(commonProfiles);
        result.addAll(currentExtProfiles);
        NbGradleConfiguration.sortProfiles(result);

        return result;
    }

    @Override
    public NbGradleConfiguration getActiveConfiguration() {
        NbGradleConfigProvider commonConfig = getCommonConfig();
        NbGradleConfiguration config = commonConfig.getActiveConfiguration();
        if (!extensionProfiles.contains(config) && !commonConfig.getConfigurations().contains(config)) {
            return NbGradleConfiguration.DEFAULT_CONFIG;
        }
        return config;
    }

    @Override
    public void setActiveConfiguration(NbGradleConfiguration configuration) throws IOException {
        getCommonConfig().setActiveConfiguration(configuration);
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
        return getCommonConfig().configurationsAffectAction(command);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener lst) {
        propertyChangeForwarder.addPropertyChangeListener(lst);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener lst) {
        propertyChangeForwarder.removePropertyChangeListener(lst);
    }

    public void removeConfiguration(NbGradleConfiguration config) {
        getCommonConfig().removeConfiguration(config);
    }

    public void addConfiguration(NbGradleConfiguration config) {
        getCommonConfig().addConfiguration(config);
    }

    public ListenerRef addActiveConfigChangeListener(Runnable listener) {
        return activeConfig.addChangeListener(listener);
    }
}
