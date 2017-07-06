package org.netbeans.gradle.project.properties;

import java.util.List;
import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileKey;

public final class ProjectProfileLoader {
    private final NbGradleSingleProjectConfigProvider configProvider;

    public ProjectProfileLoader(NbGradleSingleProjectConfigProvider configProvider) {
        this.configProvider = Objects.requireNonNull(configProvider, "configProvider");
    }

    public NbGradleCommonProperties loadCommonPropertiesForProfile(ProfileKey profileKey) {
        ActiveSettingsQuery settings = loadActiveSettingsForProfile(profileKey);
        return configProvider.getCommonProperties(settings);
    }

    private List<ProfileSettingsKey> keysWithFallbacks(ProfileKey profileKey) {
        return getProjectProfileKey(profileKey).getWithFallbacks();
    }

    public ActiveSettingsQueryEx loadActiveSettingsForProfile(ProfileKey profileKey) {
        ProfileSettingsContainer settingsContainer = configProvider.getProfileSettingsContainer();
        List<ProfileSettingsKey> combinedKeys = keysWithFallbacks(profileKey);

        List<SingleProfileSettingsEx> settings = settingsContainer.loadAllProfileSettings(combinedKeys);
        return new MultiProfileProperties(settings);
    }

    public ListenerRef loadActiveSettingsForProfile(ProfileKey profileKey, final ActiveSettingsQueryListener listener) {
        Objects.requireNonNull(listener, "listener");

        ProfileSettingsContainer settingsContainer = configProvider.getProfileSettingsContainer();
        List<ProfileSettingsKey> combinedKeys = keysWithFallbacks(profileKey);

        return settingsContainer.loadAllProfileSettings(combinedKeys, (List<SingleProfileSettingsEx> settings) -> {
            listener.onLoad(new MultiProfileProperties(settings));
        });
    }

    private ProfileSettingsContainer getProfileSettingsContainer() {
        return configProvider.getProfileSettingsContainer();
    }

    private ProfileSettingsKey getProjectProfileKey(ProfileKey profileKey) {
        return configProvider.getProjectProfileKey(profileKey);
    }

    public SingleProfileSettingsEx loadPropertiesForProfile(ProfileKey profileKey) {
        ProfileSettingsKey key = getProjectProfileKey(profileKey);
        return getProfileSettingsContainer().loadProfileSettings(key);
    }
}
