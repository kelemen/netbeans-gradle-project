package org.netbeans.gradle.project.properties2;

import javax.annotation.Nullable;

public final class ConfigSaveOption {
    private final String preferredLineSeparator;

    public ConfigSaveOption(@Nullable String preferredLineSeparator) {
        this.preferredLineSeparator = preferredLineSeparator;
    }

    @Nullable
    public String getPreferredLineSeparator() {
        return preferredLineSeparator;
    }
}
