package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;

public interface ProfileEditorFactory {
    @Nonnull
    public ProfileEditor startEditingProfile(@Nonnull ProfileInfo profileInfo, @Nonnull ActiveSettingsQuery profileQuery);
}
