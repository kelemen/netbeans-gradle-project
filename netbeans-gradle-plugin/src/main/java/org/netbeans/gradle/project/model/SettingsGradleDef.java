package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import java.util.Objects;

public final class SettingsGradleDef {
    public static final SettingsGradleDef DEFAULT = new SettingsGradleDef(null, true);
    public static final SettingsGradleDef NO_SETTINGS = new SettingsGradleDef(null, false);

    private final Path settingsGradle;
    private final boolean maySearchUpwards;

    public SettingsGradleDef(Path settingsGradle, boolean maySearchUpwards) {
        this.settingsGradle = settingsGradle;
        this.maySearchUpwards = settingsGradle == null && maySearchUpwards;
    }

    public Path getSettingsGradle() {
        return settingsGradle;
    }

    public boolean isMaySearchUpwards() {
        return maySearchUpwards;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(settingsGradle);
        hash = 37 * hash + (maySearchUpwards ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final SettingsGradleDef other = (SettingsGradleDef)obj;
        return this.maySearchUpwards == other.maySearchUpwards
                && Objects.equals(this.settingsGradle, other.settingsGradle);
    }

    @Override
    public String toString() {
        return maySearchUpwards
                ? "{" + settingsGradle + "}"
                : "{" + settingsGradle + ", may not search}";
    }
}
