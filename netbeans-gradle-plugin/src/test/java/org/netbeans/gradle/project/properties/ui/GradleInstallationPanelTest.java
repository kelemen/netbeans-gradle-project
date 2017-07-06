package org.netbeans.gradle.project.properties.ui;

import java.io.File;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;

public class GradleInstallationPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory() {
        return GradleInstallationPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleLocation().setValue(GradleLocationDef.fromVersion("1.7", true));
            input.gradleUserHomeDir().setValue(new File("my-user-home1"));
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleLocation().setValue(GradleLocationDef.fromVersion("1.9", false));
            input.gradleUserHomeDir().setValue(new File("my-user-home2"));
        });
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleLocation().setValue(new GradleLocationDef(GradleLocationDefault.DEFAULT_REF, false));
            input.gradleUserHomeDir().setValue(new File("my-user-home3"));
        });
    }

    @Test
    public void testInitAndReadBack4() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.gradleLocation().setValue(new GradleLocationDef(GradleLocationDefault.DEFAULT_REF, true));
            input.gradleUserHomeDir().setValue(new File("my-user-home4"));
        });
    }
}
