package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;

public interface GlobalSettingsProvider {
    @Nonnull
    public ActiveSettingsQuery getExtensionSettings(@Nonnull String extensionName);
}
