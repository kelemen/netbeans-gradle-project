package org.netbeans.gradle.project;

import java.io.File;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.SettingsGradleDef;

public interface GlobalSettingsFileManager {
    public SettingsGradleDef tryGetSettingsFile(File projectDir);
    public void updateSettingsFile(NbGradleModel model);
}
