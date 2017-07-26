package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.ScriptFileProvider;

public final class ModelLoadUtils {
    public static Path getSettingsGradleForProject(
            SettingsGradleDef settingsGradleDef,
            Path projectDir,
            ScriptFileProvider scriptProvider) {
        Path explicitSettingsGradle = settingsGradleDef.getSettingsGradle();
        if (explicitSettingsGradle != null) {
            return explicitSettingsGradle;
        }

        return findSettingsGradle(projectDir, scriptProvider);
    }

    public static Path findSettingsGradle(Path projectDir, ScriptFileProvider scriptProvider) {
        Path settingsGradle = scriptProvider.findScriptFile(projectDir, CommonScripts.SETTINGS_BASE_NAME);
        if (settingsGradle != null) {
            return settingsGradle;
        }

        Path parentDir = projectDir.getParent();
        if (parentDir != null) {
            return findSettingsGradle(parentDir, scriptProvider);
        }
        else {
            return null;
        }
    }

    private ModelLoadUtils() {
        throw new AssertionError();
    }
}
