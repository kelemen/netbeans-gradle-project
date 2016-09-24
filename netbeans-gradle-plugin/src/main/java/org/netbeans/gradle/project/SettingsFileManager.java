
package org.netbeans.gradle.project;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.SettingsGradleDef;

final class SettingsFileManager {
    private final File projectDir;
    private final ProjectModelUpdater<?> modelUpdater;
    private final PropertySource<NbGradleModel> currentModel;
    private final AtomicReference<Path> preferredSettingsFileRef;

    public SettingsFileManager(File projectDir, ProjectModelUpdater<?> modelUpdater, PropertySource<NbGradleModel> currentModel) {
        this.projectDir = projectDir;
        this.modelUpdater = modelUpdater;
        this.currentModel = currentModel;
        this.preferredSettingsFileRef = new AtomicReference<>(tryGetPreferredSettingsFile(projectDir));
    }

    private Path getPreferredSettingsFile() {
        return preferredSettingsFileRef.get();
    }

    public SettingsGradleDef getPreferredSettingsGradleDef() {
        return new SettingsGradleDef(getPreferredSettingsFile(), !currentModel.getValue().isRootWithoutSettingsGradle());
    }

    private void updateSettingsFile(Path settingsFile) {
        Path prevSettingsFile = preferredSettingsFileRef.getAndSet(settingsFile);
        if (Objects.equals(prevSettingsFile, settingsFile)) {
            return;
        }
        modelUpdater.reloadProjectMayUseCache();
    }

    public void updateSettingsFile() {
        updateSettingsFile(tryGetPreferredSettingsFile(projectDir));
    }

    private static Path tryGetPreferredSettingsFile(File projectDir) {
        if (NbGradleModel.isBuildSrcDirectory(projectDir)) {
            return null;
        }
        Path explicitSettingsFile = RootProjectRegistry.getDefault().tryGetSettingsFile(projectDir);
        if (explicitSettingsFile != null) {
            return explicitSettingsFile;
        }
        return NbGradleModel.findSettingsGradle(projectDir);
    }
}
