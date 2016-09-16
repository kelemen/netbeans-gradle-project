package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class PlatformPriorityPanelTest {
    private static NbSupplier<GlobalSettingsPage> settingsPageFactory(final boolean hasOwnButtons) {
        return new NbSupplier<GlobalSettingsPage>() {
            @Override
            public GlobalSettingsPage get() {
                return PlatformPriorityPanel.createSettingsPage(hasOwnButtons);
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(false), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                        JavaPlatform.getDefault())));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(true), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                        JavaPlatform.getDefault())));
            }
        });
    }
}
