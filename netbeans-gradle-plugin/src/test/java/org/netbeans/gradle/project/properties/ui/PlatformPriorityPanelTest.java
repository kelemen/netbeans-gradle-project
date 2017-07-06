package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.PlatformOrder;

public class PlatformPriorityPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory(final boolean hasOwnButtons) {
        return () -> PlatformPriorityPanel.createSettingsPage(hasOwnButtons);
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(false), (input) -> {
            input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                    JavaPlatform.getDefault())));
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(true), (input) -> {
            input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                    JavaPlatform.getDefault())));
        });
    }
}
