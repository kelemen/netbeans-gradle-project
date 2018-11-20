package org.netbeans.gradle.project.properties.ui;

import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;

public class BuildScriptParsingPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory() {
        return BuildScriptParsingPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (CommonGlobalSettings input) -> {
            input.loadRootProjectFirst().setValue(false);
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (CommonGlobalSettings input) -> {
            input.loadRootProjectFirst().setValue(true);
        });
    }
}
