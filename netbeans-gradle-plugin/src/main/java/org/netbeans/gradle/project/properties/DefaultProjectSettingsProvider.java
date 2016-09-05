package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;

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

        if (extensionName.isEmpty()) {
            return new RootExtensionSettings(project);
        }
        else {
            return new ExtensionSettingsImpl(project, extensionName);
        }
    }

    private static final class RootExtensionSettings implements ExtensionSettings {
        private final NbGradleProject project;

        public RootExtensionSettings(NbGradleProject project) {
            ExceptionHelper.checkNotNullArgument(project, "project");
            this.project = project;
        }

        @Override
        public ActiveSettingsQuery getActiveSettings() {
            return project.getActiveSettingsQuery();
        }

        @Override
        public ActiveSettingsQuery loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile) {
            return project.loadActiveSettingsForProfile(profile);
        }

        @Override
        public void loadSettingsForProfile(CancellationToken cancelToken, ProfileKey profile, ActiveSettingsQueryListener settingsQueryListener) {
            project.loadActiveSettingsForProfile(profile, settingsQueryListener);
        }
    }

    private static final class ExtensionSettingsImpl implements ExtensionSettings {
        private final NbGradleProject project;
        private final ExtensionActiveSettingsQuery activeSettings;
        private final String extensionName;

        public ExtensionSettingsImpl(NbGradleProject project, String extensionName) {
            this.project = project;
            this.activeSettings = new ExtensionActiveSettingsQuery(project.getActiveSettingsQuery(), extensionName);
            this.extensionName = extensionName;
        }

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
    }
}
