package org.netbeans.gradle.project.properties.ui;

import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbSupplier;

public class OtherOptionsPanelTest {
    private static NbSupplier<GlobalSettingsPage> settingsPageFactory() {
        return OtherOptionsPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.detectProjectDependenciesByJarName().setValue(true);
            input.compileOnSave().setValue(false);
            input.projectCacheSize().setValue(251);
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.detectProjectDependenciesByJarName().setValue(false);
            input.compileOnSave().setValue(true);
            input.projectCacheSize().setValue(251);
        });
    }
}
