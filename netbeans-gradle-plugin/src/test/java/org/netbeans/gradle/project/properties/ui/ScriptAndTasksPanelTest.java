package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.NbConsumer;

public class ScriptAndTasksPanelTest {
    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(ScriptAndTasksPanel.class, new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.defaultJdk().setValue(JavaPlatform.getDefault());
                input.gradleArgs().setValue(Arrays.asList("arg1", "arg2"));
                input.gradleJvmArgs().setValue(Arrays.asList("j-arg1", "j-arg2"));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(ScriptAndTasksPanel.class, new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.defaultJdk().setValue(JavaPlatform.getDefault());
                input.gradleArgs().setValue(Collections.<String>emptyList());
                input.gradleJvmArgs().setValue(Collections.<String>emptyList());
            }
        });
    }
}
