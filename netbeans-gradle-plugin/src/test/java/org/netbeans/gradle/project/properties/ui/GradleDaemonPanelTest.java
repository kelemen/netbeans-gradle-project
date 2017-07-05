package org.netbeans.gradle.project.properties.ui;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbSupplier;

public class GradleDaemonPanelTest {
    private static NbSupplier<GlobalSettingsPage> settingsPageFactory() {
        return GradleDaemonPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleDaemonTimeoutSec().setValue((int)TimeUnit.MINUTES.toSeconds(37));
        });
    }
}
