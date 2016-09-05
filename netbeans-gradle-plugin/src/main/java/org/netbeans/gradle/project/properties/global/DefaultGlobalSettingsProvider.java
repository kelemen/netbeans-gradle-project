package org.netbeans.gradle.project.properties.global;

import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.GlobalSettingsProvider;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;

@org.openide.util.lookup.ServiceProvider(service = GlobalSettingsProvider.class)
public class DefaultGlobalSettingsProvider implements GlobalSettingsProvider {
    @Override
    public ActiveSettingsQuery getExtensionSettings(String extensionName) {
        ActiveSettingsQuery rootQuery = CommonGlobalSettings.getDefaultActiveSettingsQuery();
        return new ExtensionActiveSettingsQuery(rootQuery, extensionName);
    }
}
