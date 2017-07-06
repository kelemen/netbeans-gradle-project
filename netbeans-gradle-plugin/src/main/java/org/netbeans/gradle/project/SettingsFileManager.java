package org.netbeans.gradle.project;

import java.io.File;
import java.util.Objects;
import org.netbeans.gradle.project.model.SettingsGradleDef;

final class SettingsFileManager {
    private final File projectDir;
    private final GlobalSettingsFileManager globalManager;

    public SettingsFileManager(
            File projectDir,
            GlobalSettingsFileManager globalManager) {
        this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
        this.globalManager = Objects.requireNonNull(globalManager, "globalManager");
    }

    public SettingsGradleDef getPreferredSettingsGradleDef() {
        SettingsGradleDef result = globalManager.tryGetSettingsFile(projectDir);
        return result != null ? result : SettingsGradleDef.DEFAULT;
    }
}
