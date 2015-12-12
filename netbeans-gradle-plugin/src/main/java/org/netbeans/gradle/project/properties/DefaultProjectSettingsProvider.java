package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.config.PropertyDef;

public final class DefaultProjectSettingsProvider implements ProjectSettingsProvider {
    private final NbGradleProject project;
    private final AtomicReference<NbGradleSingleProjectConfigProvider> configProviderRef;

    public DefaultProjectSettingsProvider(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
        this.configProviderRef = new AtomicReference<>(null);
    }

    public NbGradleSingleProjectConfigProvider getConfigProvider() {
        NbGradleSingleProjectConfigProvider result = configProviderRef.get();
        if (result == null) {
            result = project.getLookup().lookup(NbGradleSingleProjectConfigProvider.class);
            configProviderRef.set(result);
        }
        return result;
    }

    @Override
    public Collection<ProfileDef> getCurrentProfileDefs() {
        Collection<NbGradleConfiguration> configs = getConfigProvider().getConfigurations();
        List<ProfileDef> result = new ArrayList<>(configs.size());
        for (NbGradleConfiguration config: configs) {
            result.add(config.getProfileDef());
        }
        return result;
    }

    @Override
    public ExtensionSettings getExtensionSettings(final String extensionName) {
        ExceptionHelper.checkNotNullArgument(extensionName, "extensionName");

        final ExtensionActiveSettingsQuery activeSettings = new ExtensionActiveSettingsQuery(project.getActiveSettingsQuery(), extensionName);
        return new ExtensionSettings() {
            @Override
            public ActiveSettingsQuery getActiveSettings() {
                return activeSettings;
            }

            @Override
            public ActiveSettingsQuery loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile) {
                ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

                ActiveSettingsQueryEx rootSettings = project.loadActiveSettingsForProfile(profile);
                return new ExtensionActiveSettingsQuery(rootSettings, extensionName);
            }

            @Override
            public void loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile, final ActiveSettingsQueryListener settingsQueryListener) {
                ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
                ExceptionHelper.checkNotNullArgument(settingsQueryListener, "settingsQueryListener");

                project.loadActiveSettingsForProfile(profile, new ActiveSettingsQueryListener() {
                    @Override
                    public void onLoad(ActiveSettingsQuery settings) {
                        settingsQueryListener.onLoad(new ExtensionActiveSettingsQuery(settings, extensionName));
                    }
                });
            }
        };
    }

    private static <ValueKey, ValueType> PropertyDef<ValueKey, ValueType> toExtensionDef(
            PropertyDef<ValueKey, ValueType> propertyDef,
            String extensionName) {
        return propertyDef.withParentConfigPath("extensions", extensionName);
    }

    private static final class ExtensionActiveSettingsQuery implements ActiveSettingsQuery {
        private final ActiveSettingsQuery rootQuery;
        private final String extensionName;

        public ExtensionActiveSettingsQuery(ActiveSettingsQuery rootQuery, String extensionName) {
            this.rootQuery = rootQuery;
            this.extensionName = extensionName;
        }

        @Override
        public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
            return rootQuery.getProperty(toExtensionDef(propertyDef, extensionName));
        }

        @Override
        public PropertySource<SingleProfileSettings> currentProfileSettings() {
            final PropertySource<SingleProfileSettings> rootSettings = rootQuery.currentProfileSettings();
            return new PropertySource<SingleProfileSettings>() {
                @Override
                public SingleProfileSettings getValue() {
                    return new ExtensionSingleProfileSettings(rootSettings.getValue(), extensionName);
                }

                @Override
                public ListenerRef addChangeListener(Runnable listener) {
                    return rootSettings.addChangeListener(listener);
                }
            };
        }
    }

    private static final class ExtensionSingleProfileSettings implements SingleProfileSettings {
        private final SingleProfileSettings rootSettings;
        private final String extensionName;

        public ExtensionSingleProfileSettings(SingleProfileSettings rootSettings, String extensionName) {
            this.rootSettings = rootSettings;
            this.extensionName = extensionName;
        }

        @Override
        public ProfileKey getKey() {
            return rootSettings.getKey();
        }

        @Override
        public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(PropertyDef<ValueKey, ValueType> propertyDef) {
            return rootSettings.getProperty(toExtensionDef(propertyDef, extensionName));
        }
    }
}
