package org.netbeans.gradle.project.properties.global;

import java.util.Collections;
import java.util.List;
import org.netbeans.gradle.project.properties.LoadableSingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.ProfileSettingsKey;

public enum GlobalProfileSettingsKey implements ProfileSettingsKey {
    GLOBAL_DEFAULTS_KEY;

    @Override
    public List<ProfileSettingsKey> getWithFallbacks() {
        return Collections.<ProfileSettingsKey>singletonList(GLOBAL_DEFAULTS_KEY);
    }

    @Override
    public LoadableSingleProfileSettingsEx openUnloadedProfileSettings() {
        return GlobalProfileSettings.getInstance();
    }
}
