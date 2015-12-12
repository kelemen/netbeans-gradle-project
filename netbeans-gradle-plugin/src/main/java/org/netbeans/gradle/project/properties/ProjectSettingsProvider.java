package org.netbeans.gradle.project.properties;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.cancel.CancellationToken;
import org.netbeans.gradle.project.api.config.ProfileDef;

public interface ProjectSettingsProvider {
    @Nonnull
    public Collection<ProfileDef> getCurrentProfileDefs();

    @Nonnull
    public ExtensionSettings getExtensionSettings(@Nonnull String extensionName);

    public static interface ExtensionSettings {
        @Nonnull
        public ActiveSettingsQuery getActiveSettings();

        @Nonnull
        public ActiveSettingsQuery loadSettingsForProfile(
                @Nonnull CancellationToken cancelToken,
                @Nullable ProfileKey profile);

        @Nonnull
        public void loadSettingsForProfile(
                @Nonnull CancellationToken cancelToken,
                @Nullable ProfileKey profile,
                @Nonnull ActiveSettingsQueryListener settingsQueryListener);
    }
}
