package org.netbeans.gradle.project;

import java.io.File;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.SettingsGradleDef;

final class SettingsFileManager {
    private final File projectDir;
    private final GlobalSettingsFileManager globalManager;

    public SettingsFileManager(
            File projectDir,
            GlobalSettingsFileManager globalManager) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
        ExceptionHelper.checkNotNullArgument(globalManager, "globalManager");

        this.projectDir = projectDir;
        this.globalManager = globalManager;
    }

    public SettingsGradleDef getPreferredSettingsGradleDef() {
        SettingsGradleDef result = globalManager.tryGetSettingsFile(projectDir);
        return result != null ? result : SettingsGradleDef.DEFAULT;
    }
}
