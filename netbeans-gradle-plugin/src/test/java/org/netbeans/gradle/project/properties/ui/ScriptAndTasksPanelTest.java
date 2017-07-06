package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;

public class ScriptAndTasksPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory() {
        return ScriptAndTasksPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.defaultJdk().setValue(ScriptPlatform.getDefault());
            input.gradleArgs().setValue(Arrays.asList("arg1", "arg2"));
            input.gradleJvmArgs().setValue(Arrays.asList("j-arg1", "j-arg2"));
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.defaultJdk().setValue(ScriptPlatform.getDefault());
            input.gradleArgs().setValue(Collections.<String>emptyList());
            input.gradleJvmArgs().setValue(Collections.<String>emptyList());
        });
    }
}
