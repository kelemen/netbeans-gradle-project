package org.netbeans.gradle.project.properties.ui;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;

public class GradleDaemonPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory() {
        return GradleDaemonPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleDaemonTimeoutSec().setValue((int)TimeUnit.MINUTES.toSeconds(37));
        });
    }
}
