package org.netbeans.gradle.project.properties;

import javax.annotation.Nullable;

public final class ConfigSaveOptions {
    private final String preferredLineSeparator;

    public ConfigSaveOptions(@Nullable String preferredLineSeparator) {
        this.preferredLineSeparator = preferredLineSeparator;
    }

    @Nullable
    public String getPreferredLineSeparator() {
        return preferredLineSeparator;
    }
}
