package org.netbeans.gradle.project.properties.ui;

import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.global.SelfMaintainedTasks;
import org.netbeans.gradle.project.util.NbConsumer;

public class TaskExecutionPanelTest {
    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(TaskExecutionPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.alwaysClearOutput().setValue(true);
                input.selfMaintainedTasks().setValue(SelfMaintainedTasks.TRUE);
                input.skipTests().setValue(true);
                input.skipCheck().setValue(true);
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(TaskExecutionPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.alwaysClearOutput().setValue(false);
                input.selfMaintainedTasks().setValue(SelfMaintainedTasks.FALSE);
                input.skipTests().setValue(false);
                input.skipCheck().setValue(false);
            }
        });
    }
}
