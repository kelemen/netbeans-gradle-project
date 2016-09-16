package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class ScriptAndTasksPanelTest {
    private static NbSupplier<GlobalSettingsPage> settingsPageFactory() {
        return new NbSupplier<GlobalSettingsPage>() {
            @Override
            public GlobalSettingsPage get() {
                return ScriptAndTasksPanel.createSettingsPage();
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.defaultJdk().setValue(ScriptPlatform.getDefault());
                input.gradleArgs().setValue(Arrays.asList("arg1", "arg2"));
                input.gradleJvmArgs().setValue(Arrays.asList("j-arg1", "j-arg2"));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.defaultJdk().setValue(ScriptPlatform.getDefault());
                input.gradleArgs().setValue(Collections.<String>emptyList());
                input.gradleJvmArgs().setValue(Collections.<String>emptyList());
            }
        });
    }
}
