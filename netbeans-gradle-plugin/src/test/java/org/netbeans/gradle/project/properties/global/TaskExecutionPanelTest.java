package org.netbeans.gradle.project.properties.global;

import org.junit.Test;
import org.netbeans.gradle.project.util.NbConsumer;

public class TaskExecutionPanelTest {
    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(TaskExecutionPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.alwaysClearOutput().setValue(true);
                input.omitInitScript().setValue(true);
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
                input.omitInitScript().setValue(false);
                input.skipTests().setValue(false);
                input.skipCheck().setValue(false);
            }
        });
    }
}
