package org.netbeans.gradle.project.properties.global;

import java.util.Arrays;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class PlatformPriorityPanelTest {
    private static NbSupplier<PlatformPriorityPanel> getFactory(final boolean hasOwnButtons) {
        return new NbSupplier<PlatformPriorityPanel>() {
            @Override
            public PlatformPriorityPanel get() {
                return new PlatformPriorityPanel(hasOwnButtons);
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(getFactory(false), new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                        JavaPlatform.getDefault())));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(getFactory(true), new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.platformPreferenceOrder().setValue(new PlatformOrder(Arrays.asList(
                        JavaPlatform.getDefault())));
            }
        });
    }
}
