package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;

public final class DefaultProjectSettingsProvider implements ProjectSettingsProvider {
    private final NbGradleSingleProjectConfigProvider configProvider;
    private final ProjectProfileLoader profileLoader;

    public DefaultProjectSettingsProvider(
            NbGradleSingleProjectConfigProvider configProvider,
            ProjectProfileLoader profileLoader) {
        this.configProvider = Objects.requireNonNull(configProvider, "configProvider");
        this.profileLoader = Objects.requireNonNull(profileLoader, "profileLoader");
    }

    @Override
    public Collection<ProfileDef> getCurrentProfileDefs() {
        Collection<NbGradleConfiguration> configs = configProvider.getConfigurations();
        List<ProfileDef> result = new ArrayList<>(configs.size());
        for (NbGradleConfiguration config: configs) {
            result.add(config.getProfileDef());
        }
        return result;
    }

    @Override
    public ExtensionSettings getExtensionSettings(String extensionName) {
        Objects.requireNonNull(extensionName, "extensionName");

        if (extensionName.isEmpty()) {
            return new RootExtensionSettings(configProvider, profileLoader);
        }
        else {
            return new ExtensionSettingsImpl(configProvider, profileLoader, extensionName);
        }
    }

    private static final class RootExtensionSettings implements ExtensionSettings {
        private final NbGradleSingleProjectConfigProvider configProvider;
        private final ProjectProfileLoader profileLoader;

        public RootExtensionSettings(
                NbGradleSingleProjectConfigProvider configProvider,
                ProjectProfileLoader profileLoader) {
            this.configProvider = configProvider;
            this.profileLoader = profileLoader;
        }

        @Override
        public ActiveSettingsQuery getActiveSettings() {
            return configProvider.getActiveSettingsQuery();
        }

        @Override
        public ActiveSettingsQuery loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile) {
            return profileLoader.loadActiveSettingsForProfile(profile);
        }

        @Override
        public void loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile, ActiveSettingsQueryListener settingsQueryListener) {
            profileLoader.loadActiveSettingsForProfile(profile, settingsQueryListener);
        }
    }

    private static final class ExtensionSettingsImpl implements ExtensionSettings {
        private final ProjectProfileLoader profileLoader;
        private final ExtensionActiveSettingsQuery activeSettings;
        private final String extensionName;

        public ExtensionSettingsImpl(
                NbGradleSingleProjectConfigProvider configProvider,
                ProjectProfileLoader profileLoader,
                String extensionName) {
            this.activeSettings = new ExtensionActiveSettingsQuery(
                    configProvider.getActiveSettingsQuery(),
                    extensionName);
            this.profileLoader = profileLoader;
            this.extensionName = extensionName;
        }

        @Override
        public ActiveSettingsQuery getActiveSettings() {
            return activeSettings;
        }

        @Override
        public ActiveSettingsQuery loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile) {
            Objects.requireNonNull(cancelToken, "cancelToken");

            ActiveSettingsQueryEx rootSettings = profileLoader.loadActiveSettingsForProfile(profile);
            return new ExtensionActiveSettingsQuery(rootSettings, extensionName);
        }

        @Override
        public void loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile, final ActiveSettingsQueryListener settingsQueryListener) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(settingsQueryListener, "settingsQueryListener");

            profileLoader.loadActiveSettingsForProfile(profile, (ActiveSettingsQuery settings) -> {
                settingsQueryListener.onLoad(new ExtensionActiveSettingsQuery(settings, extensionName));
            });
        }
    }
}
