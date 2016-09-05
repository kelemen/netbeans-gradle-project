package org.netbeans.gradle.project.properties.global;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.GradleGlobalSettingsProvider;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;

@org.openide.util.lookup.ServiceProvider(service = GradleGlobalSettingsProvider.class)
public class DefaultGradleGlobalSettingsProvider implements GradleGlobalSettingsProvider {
    @Override
    public ActiveSettingsQuery getExtensionSettings(String extensionName) {
        ActiveSettingsQuery rootQuery = CommonGlobalSettings.getDefaultActiveSettingsQuery();
        return new ExtensionActiveSettingsQuery(rootQuery, extensionName);
    }
}
