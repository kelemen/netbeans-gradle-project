package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.util.NbConsumer;

public class ScriptAndTasksPanelTest {
    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(ScriptAndTasksPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleJdk().setValue(JavaPlatform.getDefault());
                input.gradleArgs().setValue(Arrays.asList("arg1", "arg2"));
                input.gradleJvmArgs().setValue(Arrays.asList("j-arg1", "j-arg2"));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(ScriptAndTasksPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleJdk().setValue(JavaPlatform.getDefault());
                input.gradleArgs().setValueFromString("");
                input.gradleJvmArgs().setValueFromString("");
            }
        });
    }
}
