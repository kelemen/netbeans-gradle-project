package org.netbeans.gradle.project.api.config.ui;

import org.jtrim.utils.ExceptionHelper;

public final class ProfileBasedForeignSettingsCategory {
    private final String extensionName;
    private final ProfileBasedSettingsCategory settingsCetegory;

    public ProfileBasedForeignSettingsCategory(String extensionName, ProfileBasedSettingsCategory settingsCetegory) {
        ExceptionHelper.checkNotNullArgument(extensionName, "extensionName");
        ExceptionHelper.checkNotNullArgument(settingsCetegory, "settingsCetegory");

        this.extensionName = extensionName;
        this.settingsCetegory = settingsCetegory;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public ProfileBasedSettingsCategory getSettingsCetegory() {
        return settingsCetegory;
    }
}
